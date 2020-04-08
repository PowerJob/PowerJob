package com.github.kfcfans.oms.server.service;

import akka.actor.ActorSelection;
import com.github.kfcfans.common.ExecuteType;
import com.github.kfcfans.common.ProcessorType;
import com.github.kfcfans.common.request.ServerScheduleJobReq;
import com.github.kfcfans.oms.server.core.akka.OhMyServer;
import com.github.kfcfans.oms.server.persistence.model.JobInfoDO;
import com.github.kfcfans.oms.server.persistence.repository.ExecuteLogRepository;
import com.github.kfcfans.oms.server.service.ha.WorkerManagerService;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

    // 前三个状态都视为运行中
    private static final List<Integer> runningStatus = Lists.newArrayList(WAITING_DISPATCH.getV(), WAITING_WORKER_RECEIVE.getV(), RUNNING.getV());

    private static final String TOO_MUCH_REASON = "too much instance(%d>%d)";
    private static final String NO_WORKER_REASON = "no worker available";
    private static final String EMPTY_RESULT = "";

    public void dispatch(JobInfoDO jobInfo, long instanceId, long currentRunningTimes) {

        log.debug("[DispatchService] start to dispatch job -> {}.", jobInfo);

        // 查询当前运行的实例数
        long runningInstanceCount = executeLogRepository.countByJobIdAndStatusIn(jobInfo.getId(), runningStatus);

        // 超出最大同时运行限制，不执行调度
        if (runningInstanceCount > jobInfo.getMaxInstanceNum()) {
            String result = String.format(TOO_MUCH_REASON, runningInstanceCount, jobInfo.getMaxInstanceNum());
            log.warn("[DispatchService] cancel dispatch job({}) due to too much instance(num={}) is running.", jobInfo, runningInstanceCount);
            executeLogRepository.update4Trigger(instanceId, FAILED.getV(), currentRunningTimes, result);

            return;
        }

        // 获取 Worker
        String taskTrackerAddress = WorkerManagerService.chooseBestWorker(jobInfo.getAppId());
        List<String> allAvailableWorker = WorkerManagerService.getAllAvailableWorker(jobInfo.getAppId());

        if (StringUtils.isEmpty(taskTrackerAddress)) {
            log.warn("[DispatchService] cancel dispatch job({}) due to no worker available.", jobInfo);
            executeLogRepository.update4Trigger(instanceId, FAILED.getV(), currentRunningTimes, NO_WORKER_REASON);
            return;
        }

        // 消除非原子操作带来的潜在不一致
        allAvailableWorker.remove(taskTrackerAddress);
        allAvailableWorker.add(taskTrackerAddress);

        // 构造请求
        ServerScheduleJobReq req = new ServerScheduleJobReq();
        req.setAllWorkerAddress(allAvailableWorker);
        req.setJobId(jobInfo.getId());
        req.setInstanceId(instanceId);

        req.setExecuteType(ExecuteType.of(jobInfo.getExecuteType()).name());
        req.setProcessorType(ProcessorType.of(jobInfo.getProcessorType()).name());
        req.setProcessorInfo(jobInfo.getProcessorInfo());

        req.setInstanceTimeoutMS(jobInfo.getInstanceTimeLimit());
        req.setTaskTimeoutMS(jobInfo.getTaskTimeLimit());

        req.setJobParams(jobInfo.getJobParams());
        req.setThreadConcurrency(jobInfo.getConcurrency());
        req.setTaskRetryNum(jobInfo.getTaskRetryNum());

        // 发送请求（不可靠，需要一个后台线程定期轮询状态）
        ActorSelection taskTrackerActor = OhMyServer.getTaskTrackerActor(taskTrackerAddress);
        taskTrackerActor.tell(req, null);
        log.debug("[DispatchService] send request({}) to TaskTracker({}) succeed.", req, taskTrackerActor.pathString());

        // 修改状态
        executeLogRepository.update4Trigger(instanceId, WAITING_WORKER_RECEIVE.getV(), currentRunningTimes + 1, EMPTY_RESULT);
    }
}
