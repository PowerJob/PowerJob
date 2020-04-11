package com.github.kfcfans.oms.server.service;

import akka.actor.ActorSelection;
import com.github.kfcfans.common.*;
import com.github.kfcfans.common.request.ServerScheduleJobReq;
import com.github.kfcfans.oms.server.akka.OhMyServer;
import com.github.kfcfans.oms.server.persistence.model.JobInfoDO;
import com.github.kfcfans.oms.server.persistence.repository.ExecuteLogRepository;
import com.github.kfcfans.oms.server.service.ha.WorkerManagerService;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

import static com.github.kfcfans.common.InstanceStatus.*;


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
    private ExecuteLogRepository executeLogRepository;

    private static final String EMPTY_RESULT = "";

    /**
     * 将任务从Server派发到Worker（TaskTracker）
     * @param jobInfo 任务的元信息
     * @param instanceId 任务实例ID
     * @param currentRunningTimes 当前运行的次数
     */
    public void dispatch(JobInfoDO jobInfo, long instanceId, long currentRunningTimes) {

        Long jobId = jobInfo.getId();
        log.info("[DispatchService] start to dispatch job: {}.", jobInfo);
        // 查询当前运行的实例数
        long current = System.currentTimeMillis();
        long runningInstanceCount = executeLogRepository.countByJobIdAndStatusIn(jobId, generalizedRunningStatus);

        // 超出最大同时运行限制，不执行调度
        if (runningInstanceCount > jobInfo.getMaxInstanceNum()) {
            String result = String.format(SystemInstanceResult.TOO_MUCH_INSTANCE, runningInstanceCount, jobInfo.getMaxInstanceNum());
            log.warn("[DispatchService] cancel dispatch job(jobId={}) due to too much instance(num={}) is running.", jobId, runningInstanceCount);
            executeLogRepository.update4Trigger(instanceId, FAILED.getV(), currentRunningTimes, current, RemoteConstant.EMPTY_ADDRESS, result);
            return;
        }

        // 获取 Worker
        String taskTrackerAddress = WorkerManagerService.chooseBestWorker(jobInfo.getAppId());
        List<String> allAvailableWorker = WorkerManagerService.getAllAvailableWorker(jobInfo.getAppId());

        if (StringUtils.isEmpty(taskTrackerAddress)) {
            String clusterStatusDescription = WorkerManagerService.getWorkerClusterStatusDescription(jobInfo.getAppId());
            log.warn("[DispatchService] cancel dispatch job(jobId={}) due to no worker available, clusterStatus is {}.", jobId, clusterStatusDescription);
            executeLogRepository.update4Trigger(instanceId, FAILED.getV(), currentRunningTimes, current, RemoteConstant.EMPTY_ADDRESS, SystemInstanceResult.NO_WORKER_AVAILABLE);
            return;
        }

        // 消除非原子操作带来的潜在不一致
        allAvailableWorker.remove(taskTrackerAddress);
        allAvailableWorker.add(taskTrackerAddress);

        // 构造请求
        ServerScheduleJobReq req = new ServerScheduleJobReq();
        BeanUtils.copyProperties(jobInfo, req);
        req.setInstanceId(instanceId);
        req.setAllWorkerAddress(allAvailableWorker);

        req.setExecuteType(ExecuteType.of(jobInfo.getExecuteType()).name());
        req.setProcessorType(ProcessorType.of(jobInfo.getProcessorType()).name());
        req.setTimeExpressionType(TimeExpressionType.of(jobInfo.getTimeExpressionType()).name());

        req.setInstanceTimeoutMS(jobInfo.getInstanceTimeLimit());

        req.setThreadConcurrency(jobInfo.getConcurrency());

        // 发送请求（不可靠，需要一个后台线程定期轮询状态）
        ActorSelection taskTrackerActor = OhMyServer.getTaskTrackerActor(taskTrackerAddress);
        taskTrackerActor.tell(req, null);
        log.debug("[DispatchService] send request({}) to TaskTracker({}) succeed.", req, taskTrackerActor.pathString());

        // 修改状态
        executeLogRepository.update4Trigger(instanceId, WAITING_WORKER_RECEIVE.getV(), currentRunningTimes + 1, current, taskTrackerAddress, EMPTY_RESULT);
    }
}
