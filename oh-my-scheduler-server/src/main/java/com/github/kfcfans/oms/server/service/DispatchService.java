package com.github.kfcfans.oms.server.service;

import akka.actor.ActorSelection;
import com.github.kfcfans.oms.common.*;
import com.github.kfcfans.oms.common.request.ServerScheduleJobReq;
import com.github.kfcfans.oms.server.akka.OhMyServer;
import com.github.kfcfans.oms.server.persistence.core.model.InstanceInfoDO;
import com.github.kfcfans.oms.server.persistence.core.model.JobInfoDO;
import com.github.kfcfans.oms.server.persistence.core.repository.InstanceInfoRepository;
import com.github.kfcfans.oms.server.service.ha.WorkerManagerService;
import com.github.kfcfans.oms.server.service.instance.InstanceManager;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;

import static com.github.kfcfans.oms.common.InstanceStatus.*;


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
    private InstanceInfoRepository instanceInfoRepository;

    private static final Splitter commaSplitter = Splitter.on(",");

    public void redispatch(JobInfoDO jobInfo, long instanceId, long currentRunningTimes) {
        InstanceInfoDO instanceInfo = instanceInfoRepository.findByInstanceId(instanceId);
        dispatch(jobInfo, instanceId, currentRunningTimes, instanceInfo.getInstanceParams(), instanceInfo.getWorkflowId());
    }

    /**
     * 将任务从Server派发到Worker（TaskTracker）
     * @param jobInfo 任务的元信息
     * @param instanceId 任务实例ID
     * @param currentRunningTimes 当前运行的次数
     * @param instanceParams 实例的运行参数，API触发方式专用
     * @param wfInstanceId 工作流任务实例ID，workflow 任务专用
     */
    public void dispatch(JobInfoDO jobInfo, long instanceId, long currentRunningTimes, String instanceParams, Long wfInstanceId) {
        Long jobId = jobInfo.getId();
        log.info("[DispatchService] start to dispatch job: {};instancePrams: {}.", jobInfo, instanceParams);

        String dbInstanceParams = instanceParams == null ? "" : instanceParams;

        // 查询当前运行的实例数
        long current = System.currentTimeMillis();
        long runningInstanceCount = instanceInfoRepository.countByJobIdAndStatusIn(jobId, generalizedRunningStatus);

        // 超出最大同时运行限制，不执行调度
        if (runningInstanceCount > jobInfo.getMaxInstanceNum()) {
            String result = String.format(SystemInstanceResult.TOO_MUCH_INSTANCE, runningInstanceCount, jobInfo.getMaxInstanceNum());
            log.warn("[DispatchService] cancel dispatch job(jobId={}) due to too much instance(num={}) is running.", jobId, runningInstanceCount);
            instanceInfoRepository.update4TriggerFailed(instanceId, FAILED.getV(), currentRunningTimes, current, current, RemoteConstant.EMPTY_ADDRESS, result, dbInstanceParams);
            return;
        }

        // 获取当前所有可用的Worker
        List<String> allAvailableWorker = WorkerManagerService.getSortedAvailableWorker(jobInfo.getAppId(), jobInfo.getMinCpuCores(), jobInfo.getMinMemorySpace(), jobInfo.getMinDiskSpace());

        // 筛选指定的机器
        List<String> finalWorkers = Lists.newLinkedList();
        if (!StringUtils.isEmpty(jobInfo.getDesignatedWorkers())) {
            Set<String> designatedWorkers = Sets.newHashSet(commaSplitter.splitToList(jobInfo.getDesignatedWorkers()));
            for (String av : allAvailableWorker) {
                if (designatedWorkers.contains(av)) {
                    finalWorkers.add(av);
                }
            }
        }else {
            finalWorkers = allAvailableWorker;
        }

        if (CollectionUtils.isEmpty(finalWorkers)) {
            String clusterStatusDescription = WorkerManagerService.getWorkerClusterStatusDescription(jobInfo.getAppId());
            log.warn("[DispatchService] cancel dispatch job(jobId={}) due to no worker available, clusterStatus is {}.", jobId, clusterStatusDescription);
            instanceInfoRepository.update4TriggerFailed(instanceId, FAILED.getV(), currentRunningTimes, current, current, RemoteConstant.EMPTY_ADDRESS, SystemInstanceResult.NO_WORKER_AVAILABLE, dbInstanceParams);
            return;
        }

        // 限定集群大小（0代表不限制）
        if (jobInfo.getMaxWorkerCount() > 0) {
            if (finalWorkers.size() > jobInfo.getMaxWorkerCount()) {
                finalWorkers = finalWorkers.subList(0, jobInfo.getMaxWorkerCount());
            }
        }

        // 注册到任务实例管理中心
        InstanceManager.register(instanceId, jobInfo);

        // 构造请求
        ServerScheduleJobReq req = new ServerScheduleJobReq();
        BeanUtils.copyProperties(jobInfo, req);
        // 传入 JobId
        req.setJobId(jobInfo.getId());
        // 传入 InstanceParams
        if (StringUtils.isEmpty(instanceParams)) {
            req.setInstanceParams(null);
        }else {
            req.setInstanceParams(instanceParams);
        }
        req.setInstanceId(instanceId);
        req.setAllWorkerAddress(finalWorkers);

        // 设置工作流ID
        req.setWfInstanceId(wfInstanceId);

        req.setExecuteType(ExecuteType.of(jobInfo.getExecuteType()).name());
        req.setProcessorType(ProcessorType.of(jobInfo.getProcessorType()).name());
        req.setTimeExpressionType(TimeExpressionType.of(jobInfo.getTimeExpressionType()).name());

        req.setInstanceTimeoutMS(jobInfo.getInstanceTimeLimit());

        req.setThreadConcurrency(jobInfo.getConcurrency());

        // 发送请求（不可靠，需要一个后台线程定期轮询状态）
        String taskTrackerAddress = finalWorkers.get(0);
        ActorSelection taskTrackerActor = OhMyServer.getTaskTrackerActor(taskTrackerAddress);
        taskTrackerActor.tell(req, null);
        log.debug("[DispatchService] send request({}) to TaskTracker({}) succeed.", req, taskTrackerActor.pathString());

        // 修改状态
        instanceInfoRepository.update4TriggerSucceed(instanceId, WAITING_WORKER_RECEIVE.getV(), currentRunningTimes + 1, current, taskTrackerAddress, dbInstanceParams);
    }
}
