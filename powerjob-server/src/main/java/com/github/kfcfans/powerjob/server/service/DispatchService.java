package com.github.kfcfans.powerjob.server.service;

import com.github.kfcfans.powerjob.common.*;
import com.github.kfcfans.powerjob.server.remote.worker.cluster.WorkerInfo;
import com.github.kfcfans.powerjob.common.request.ServerScheduleJobReq;
import com.github.kfcfans.powerjob.server.persistence.core.model.InstanceInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.model.JobInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.repository.InstanceInfoRepository;
import com.github.kfcfans.powerjob.server.remote.worker.cluster.WorkerClusterManagerService;
import com.github.kfcfans.powerjob.server.service.instance.InstanceManager;
import com.github.kfcfans.powerjob.server.service.instance.InstanceMetadataService;
import com.github.kfcfans.powerjob.server.service.lock.local.UseSegmentLock;
import com.github.kfcfans.powerjob.server.remote.transport.TransportService;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.github.kfcfans.powerjob.common.InstanceStatus.*;


/**
 * 派送服务（将任务从Server派发到Worker）
 *
 * @author tjq
 * @since 2020/4/5
 */
@Slf4j
@Service
public class DispatchService {

    @Resource
    private TransportService transportService;

    @Resource
    private InstanceManager instanceManager;
    @Resource
    private InstanceMetadataService instanceMetadataService;
    @Resource
    private InstanceInfoRepository instanceInfoRepository;

    private static final Splitter COMMA_SPLITTER = Splitter.on(",");

    @UseSegmentLock(type = "dispatch", key = "#jobInfo.getId().intValue()", concurrencyLevel = 1024)
    public void redispatch(JobInfoDO jobInfo, long instanceId) {
        // 这里暂时保留
        dispatch(jobInfo, instanceId);
    }

    /**
     * 将任务从Server派发到Worker（TaskTracker）
     * **************************************************
     * 2021-02-03 modify by Echo009
     * 1、移除参数 当前运行次数、工作流实例ID、实例参数
     * 更改为从当前任务实例中获取获取以上信息
     * 2、移除运行次数相关的（runningTimes）处理逻辑
     * 迁移至 {@link InstanceManager#updateStatus} 中处理
     * **************************************************
     *
     * @param jobInfo    任务的元信息，注意这里传入的 jobInfo 可能为空对象
     * @param instanceId 任务实例ID
     */
    @UseSegmentLock(type = "dispatch", key = "#jobInfo.getId().intValue()", concurrencyLevel = 1024)
    public void dispatch(JobInfoDO jobInfo, long instanceId) {
        // 检查当前任务是否被取消
        InstanceInfoDO instanceInfo = instanceInfoRepository.findByInstanceId(instanceId);
        Long jobId = instanceInfo.getId();
        if (CANCELED.getV() == instanceInfo.getStatus()) {
            log.info("[Dispatcher-{}|{}] cancel dispatch due to instance has been canceled", jobId, instanceId);
            return;
        }
        // 任务信息已经被删除
        if (jobInfo.getId() == null) {
            log.warn("[Dispatcher-{}|{}] cancel dispatch due to job(id={}) has been deleted!", jobId, instanceId, jobId);
            instanceManager.processFinishedInstance(instanceId, instanceInfo.getWfInstanceId(), FAILED, "can't find job by id " + jobId);
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
        List<WorkerInfo> suitableWorkers = obtainSuitableWorkers(jobInfo);

        if (CollectionUtils.isEmpty(suitableWorkers)) {
            String clusterStatusDescription = WorkerClusterManagerService.getWorkerClusterStatusDescription(jobInfo.getAppId());
            log.warn("[Dispatcher-{}|{}] cancel dispatch job due to no worker available, clusterStatus is {}.", jobId, instanceId, clusterStatusDescription);
            instanceInfoRepository.update4TriggerFailed(instanceId, FAILED.getV(), current, current, RemoteConstant.EMPTY_ADDRESS, SystemInstanceResult.NO_WORKER_AVAILABLE, now);

            instanceManager.processFinishedInstance(instanceId, instanceInfo.getWfInstanceId(), FAILED, SystemInstanceResult.NO_WORKER_AVAILABLE);
            return;
        }
        List<String> workerIpList = suitableWorkers.stream().map(WorkerInfo::getAddress).collect(Collectors.toList());

        // 构造任务调度请求
        ServerScheduleJobReq req = constructServerScheduleJobReq(jobInfo, instanceInfo, workerIpList);


        // 发送请求（不可靠，需要一个后台线程定期轮询状态）
        WorkerInfo taskTracker = suitableWorkers.get(0);
        String taskTrackerAddress = taskTracker.getAddress();

        transportService.tell(Protocol.of(taskTracker.getProtocol()), taskTrackerAddress, req);
        log.info("[Dispatcher-{}|{}] send schedule request to TaskTracker[protocol:{},address:{}] successfully: {}.", jobId, instanceId, taskTracker.getProtocol(), taskTrackerAddress, req);

        // 修改状态
        instanceInfoRepository.update4TriggerSucceed(instanceId, WAITING_WORKER_RECEIVE.getV(), current, taskTrackerAddress, now);

        // 装载缓存
        instanceMetadataService.loadJobInfo(instanceId, jobInfo);
    }

    /**
     * 获取当前最合适的 worker 列表
     */
    private List<WorkerInfo> obtainSuitableWorkers(JobInfoDO jobInfo) {
        // 获取当前所有可用的Worker
        List<WorkerInfo> allAvailableWorker = WorkerClusterManagerService.getSortedAvailableWorkers(jobInfo.getAppId(), jobInfo.getMinCpuCores(), jobInfo.getMinMemorySpace(), jobInfo.getMinDiskSpace());

        // 筛选指定的机器
        allAvailableWorker.removeIf(worker -> {
            // 空，则全部不过滤
            if (StringUtils.isEmpty(jobInfo.getDesignatedWorkers())) {
                return false;
            }
            // 非空，只有匹配上的 worker 才不被过滤
            Set<String> designatedWorkers = Sets.newHashSet(COMMA_SPLITTER.splitToList(jobInfo.getDesignatedWorkers()));
            return !designatedWorkers.contains(worker.getAddress());
        });


        // 限定集群大小（0代表不限制）
        if (!allAvailableWorker.isEmpty() && jobInfo.getMaxWorkerCount() > 0 && allAvailableWorker.size() > jobInfo.getMaxWorkerCount()) {
            allAvailableWorker = allAvailableWorker.subList(0, jobInfo.getMaxWorkerCount());
        }
        return allAvailableWorker;
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
        req.setInstanceTimeoutMS(jobInfo.getInstanceTimeLimit());
        req.setThreadConcurrency(jobInfo.getConcurrency());
        return req;
    }
}
