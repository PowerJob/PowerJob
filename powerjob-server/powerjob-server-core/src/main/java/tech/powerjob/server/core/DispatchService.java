package tech.powerjob.server.core;

import tech.powerjob.common.RemoteConstant;
import tech.powerjob.common.SystemInstanceResult;
import tech.powerjob.common.enums.*;
import tech.powerjob.common.request.ServerScheduleJobReq;
import tech.powerjob.server.core.instance.InstanceManager;
import tech.powerjob.server.core.instance.InstanceMetadataService;
import tech.powerjob.server.core.lock.UseSegmentLock;
import tech.powerjob.server.persistence.remote.model.InstanceInfoDO;
import tech.powerjob.server.persistence.remote.model.JobInfoDO;
import tech.powerjob.server.persistence.remote.repository.InstanceInfoRepository;
import tech.powerjob.server.remote.transport.TransportService;
import tech.powerjob.server.remote.worker.WorkerClusterQueryService;
import tech.powerjob.server.common.module.WorkerInfo;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static tech.powerjob.common.enums.InstanceStatus.*;


/**
 * 派送服务（将任务从Server派发到Worker）
 *
 * @author tjq
 * @author Echo009
 * @since 2020/4/5
 */
@Slf4j
@Service
public class DispatchService {

    @Resource
    private TransportService transportService;
    @Resource
    private WorkerClusterQueryService workerClusterQueryService;

    @Resource
    private InstanceManager instanceManager;
    @Resource
    private InstanceMetadataService instanceMetadataService;
    @Resource
    private InstanceInfoRepository instanceInfoRepository;

    /**
     * 重新派发任务实例（不考虑实例当前的状态）
     *
     * @param jobInfo    任务信息（注意，这里传入的任务信息有可能为“空”）
     * @param instanceId 实例ID
     */
    @UseSegmentLock(type = "redispatch", key = "#jobInfo.getId() ?: 0", concurrencyLevel = 16)
    public void redispatch(JobInfoDO jobInfo, long instanceId) {
        InstanceInfoDO instance = instanceInfoRepository.findByInstanceId(instanceId);
        // 将状态重置为等待派发
        instance.setStatus(InstanceStatus.WAITING_DISPATCH.getV());
        instance.setGmtModified(new Date());
        instanceInfoRepository.saveAndFlush(instance);
        dispatch(jobInfo, instanceId);
    }

    /**
     * 将任务从Server派发到Worker（TaskTracker）
     * 只会派发当前状态为等待派发的任务实例
     * **************************************************
     * 2021-02-03 modify by Echo009
     * 1、移除参数 当前运行次数、工作流实例ID、实例参数
     * 更改为从当前任务实例中获取获取以上信息
     * 2、移除运行次数相关的（runningTimes）处理逻辑
     * 迁移至 {@link InstanceManager#updateStatus} 中处理
     * **************************************************
     *
     * @param jobInfo    任务的元信息
     * @param instanceId 任务实例ID
     */
    @UseSegmentLock(type = "dispatch", key = "#jobInfo.getId() ?: 0", concurrencyLevel = 1024)
    public void dispatch(JobInfoDO jobInfo, long instanceId) {
        // 检查当前任务是否被取消
        InstanceInfoDO instanceInfo = instanceInfoRepository.findByInstanceId(instanceId);
        Long jobId = instanceInfo.getJobId();
        if (CANCELED.getV() == instanceInfo.getStatus()) {
            log.info("[Dispatcher-{}|{}] cancel dispatch due to instance has been canceled", jobId, instanceId);
            return;
        }
        // 已经被派发过则不再派发
        // fix 并发场景下重复派发的问题
        if (instanceInfo.getStatus() != WAITING_DISPATCH.getV()) {
            log.info("[Dispatcher-{}|{}] cancel dispatch due to instance has been dispatched", jobId, instanceId);
            return;
        }

        Date now = new Date();
        String dbInstanceParams = instanceInfo.getInstanceParams() == null ? "" : instanceInfo.getInstanceParams();
        log.info("[Dispatcher-{}|{}] start to dispatch job: {};instancePrams: {}.", jobId, instanceId, jobInfo, dbInstanceParams);

        // 查询当前运行的实例数
        long current = System.currentTimeMillis();
        Integer maxInstanceNum = jobInfo.getMaxInstanceNum();
        // 秒级任务只派发到一台机器，具体的 maxInstanceNum 由 TaskTracker 控制
        if (TimeExpressionType.frequentTypes.contains(jobInfo.getTimeExpressionType())) {
            maxInstanceNum = 1;
        }

        // 0 代表不限制在线任务，还能省去一次 DB 查询
        if (maxInstanceNum > 0) {
            // 不统计 WAITING_DISPATCH 的状态：使用 OpenAPI 触发的延迟任务不应该统计进去（比如 delay 是 1 天）
            // 由于不统计 WAITING_DISPATCH，所以这个 runningInstanceCount 不包含本任务自身
            long runningInstanceCount = instanceInfoRepository.countByJobIdAndStatusIn(jobId, Lists.newArrayList(WAITING_WORKER_RECEIVE.getV(), RUNNING.getV()));
            // 超出最大同时运行限制，不执行调度
            if (runningInstanceCount >= maxInstanceNum) {
                String result = String.format(SystemInstanceResult.TOO_MANY_INSTANCES, runningInstanceCount, maxInstanceNum);
                log.warn("[Dispatcher-{}|{}] cancel dispatch job due to too much instance is running ({} > {}).", jobId, instanceId, runningInstanceCount, maxInstanceNum);
                instanceInfoRepository.update4TriggerFailed(instanceId, FAILED.getV(), current, current, RemoteConstant.EMPTY_ADDRESS, result, now);

                instanceManager.processFinishedInstance(instanceId, instanceInfo.getWfInstanceId(), FAILED, result);
                return;
            }
        }

        // 获取当前最合适的 worker 列表
        List<WorkerInfo> suitableWorkers = workerClusterQueryService.getSuitableWorkers(jobInfo);

        if (CollectionUtils.isEmpty(suitableWorkers)) {
            log.warn("[Dispatcher-{}|{}] cancel dispatch job due to no worker available", jobId, instanceId);
            instanceInfoRepository.update4TriggerFailed(instanceId, FAILED.getV(), current, current, RemoteConstant.EMPTY_ADDRESS, SystemInstanceResult.NO_WORKER_AVAILABLE, now);

            instanceManager.processFinishedInstance(instanceId, instanceInfo.getWfInstanceId(), FAILED, SystemInstanceResult.NO_WORKER_AVAILABLE);
            return;
        }
        List<String> workerIpList = suitableWorkers.stream().map(WorkerInfo::getAddress).collect(Collectors.toList());

        // 构造任务调度请求
        ServerScheduleJobReq req = constructServerScheduleJobReq(jobInfo, instanceInfo, workerIpList);


        // 发送请求（不可靠，需要一个后台线程定期轮询状态）
        WorkerInfo taskTracker = selectTaskTracker(jobInfo, suitableWorkers);
        String taskTrackerAddress = taskTracker.getAddress();

        transportService.tell(Protocol.of(taskTracker.getProtocol()), taskTrackerAddress, req);
        log.info("[Dispatcher-{}|{}] send schedule request to TaskTracker[protocol:{},address:{}] successfully: {}.", jobId, instanceId, taskTracker.getProtocol(), taskTrackerAddress, req);

        // 修改状态
        instanceInfoRepository.update4TriggerSucceed(instanceId, WAITING_WORKER_RECEIVE.getV(), current, taskTrackerAddress, now);

        // 装载缓存
        instanceMetadataService.loadJobInfo(instanceId, jobInfo);
    }

    /**
     * 构造任务调度请求
     */
    private ServerScheduleJobReq constructServerScheduleJobReq(JobInfoDO jobInfo, InstanceInfoDO instanceInfo, List<String> finalWorkersIpList) {
        // 构造请求
        ServerScheduleJobReq req = new ServerScheduleJobReq();
        BeanUtils.copyProperties(jobInfo, req);
        // 传入 JobId
        req.setJobId(jobInfo.getId());
        // 传入 InstanceParams
        if (StringUtils.isEmpty(instanceInfo.getInstanceParams())) {
            req.setInstanceParams(null);
        } else {
            req.setInstanceParams(instanceInfo.getInstanceParams());
        }
        // 覆盖静态参数
        if (!StringUtils.isEmpty(instanceInfo.getJobParams())) {
            req.setJobParams(instanceInfo.getJobParams());
        }
        req.setInstanceId(instanceInfo.getInstanceId());
        req.setAllWorkerAddress(finalWorkersIpList);

        // 设置工作流ID
        req.setWfInstanceId(instanceInfo.getWfInstanceId());

        req.setExecuteType(ExecuteType.of(jobInfo.getExecuteType()).name());
        req.setProcessorType(ProcessorType.of(jobInfo.getProcessorType()).name());

        req.setTimeExpressionType(TimeExpressionType.of(jobInfo.getTimeExpressionType()).name());
        if (jobInfo.getInstanceTimeLimit() != null) {
            req.setInstanceTimeoutMS(jobInfo.getInstanceTimeLimit());
        }
        req.setThreadConcurrency(jobInfo.getConcurrency());
        return req;
    }

    private WorkerInfo selectTaskTracker(JobInfoDO jobInfo, List<WorkerInfo> workerInfos) {
        DispatchStrategy dispatchStrategy = DispatchStrategy.of(jobInfo.getDispatchStrategy());
        switch (dispatchStrategy) {
            case PERFORMANCE_FIRST:
                return workerInfos.get(0);
            case RANDOM:
                return workerInfos.get(ThreadLocalRandom.current().nextInt(workerInfos.size()));
            default:
        }
        // impossible, indian java
        return workerInfos.get(0);
    }
}
