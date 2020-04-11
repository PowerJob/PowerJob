package com.github.kfcfans.oms.worker.actors;

import akka.actor.AbstractActor;
import com.github.kfcfans.oms.worker.common.constants.TaskStatus;
import com.github.kfcfans.oms.worker.core.tracker.processor.ProcessorTracker;
import com.github.kfcfans.oms.worker.core.tracker.processor.ProcessorTrackerPool;
import com.github.kfcfans.oms.worker.persistence.TaskDO;
import com.github.kfcfans.oms.worker.pojo.request.ProcessorReportTaskStatusReq;
import com.github.kfcfans.oms.worker.pojo.request.TaskTrackerStartTaskReq;
import com.github.kfcfans.oms.worker.pojo.request.TaskTrackerStopInstanceReq;
import lombok.extern.slf4j.Slf4j;

/**
 * 普通计算节点，处理来自 JobTracker 的请求
 *
 * @author tjq
 * @since 2020/3/17
 */
@Slf4j
public class ProcessorTrackerActor extends AbstractActor {

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(TaskTrackerStartTaskReq.class, this::onReceiveTaskTrackerStartTaskReq)
                .match(TaskTrackerStopInstanceReq.class, this::onReceiveTaskTrackerStopInstanceReq)
                .matchAny(obj -> log.warn("[ProcessorTrackerActor] receive unknown request: {}.", obj))
                .build();
    }

    /**
     * 处理来自TaskTracker的task执行请求
     */
    private void onReceiveTaskTrackerStartTaskReq(TaskTrackerStartTaskReq req) {
        Long jobId = req.getInstanceInfo().getJobId();
        Long instanceId = req.getInstanceInfo().getInstanceId();
        ProcessorTracker processorTracker = ProcessorTrackerPool.getProcessorTracker(instanceId, ignore -> {
            try {
                ProcessorTracker pt = new ProcessorTracker(req);
                log.info("[ProcessorTrackerActor] create ProcessorTracker for instance(jobId={}&instanceId={}) success.", jobId, instanceId);
                return pt;
            }catch (Exception e) {
                log.warn("[ProcessorTrackerActor] create ProcessorTracker for instance(jobId={}&instanceId={}) failed.", jobId, instanceId, e);

                // 直接上报失败
                ProcessorReportTaskStatusReq report = new ProcessorReportTaskStatusReq(instanceId, req.getTaskId(), TaskStatus.WORKER_PROCESS_FAILED.getValue(), e.getMessage());
                getSender().tell(report, getSelf());

            }
            return null;
        });

        // 创建失败，直接返回
        if (processorTracker == null) {
            return;
        }

        TaskDO task = new TaskDO();

        task.setTaskId(req.getTaskId());
        task.setTaskName(req.getTaskName());
        task.setTaskContent(req.getTaskContent());
        task.setFailedCnt(req.getTaskCurrentRetryNums());
        task.setSubInstanceId(req.getSubInstanceId());

        processorTracker.submitTask(task);
    }

    private void onReceiveTaskTrackerStopInstanceReq(TaskTrackerStopInstanceReq req) {

        Long instanceId = req.getInstanceId();
        ProcessorTracker processorTracker = ProcessorTrackerPool.getProcessorTracker(instanceId);
        if (processorTracker == null) {
            log.warn("[ProcessorTrackerActor] ProcessorTracker for instance(instanceId={}) already destroyed.", instanceId);
        }else {
            processorTracker.destroy();
        }
    }
}
