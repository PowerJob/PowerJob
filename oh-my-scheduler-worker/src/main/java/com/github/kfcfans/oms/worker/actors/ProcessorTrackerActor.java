package com.github.kfcfans.oms.worker.actors;

import akka.actor.AbstractActor;
import com.github.kfcfans.oms.worker.core.tracker.processor.ProcessorTracker;
import com.github.kfcfans.oms.worker.core.tracker.processor.ProcessorTrackerPool;
import com.github.kfcfans.oms.worker.persistence.TaskDO;
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
        String jobId = req.getInstanceInfo().getJobId();
        String instanceId = req.getInstanceInfo().getInstanceId();
        ProcessorTracker processorTracker = ProcessorTrackerPool.getProcessorTracker(instanceId, ignore -> {
            ProcessorTracker pt = new ProcessorTracker(req);
            log.info("[ProcessorTrackerActor] create ProcessorTracker for instance(jobId={}&instanceId={}) success.", jobId, instanceId);
            return pt;
        });

        TaskDO task = new TaskDO();

        task.setTaskId(req.getTaskId());
        task.setTaskName(req.getTaskName());
        task.setTaskContent(req.getTaskContent());
        task.setFailedCnt(req.getTaskCurrentRetryNums());

        processorTracker.submitTask(task);
    }

    private void onReceiveTaskTrackerStopInstanceReq(TaskTrackerStopInstanceReq req) {

        String instanceId = req.getInstanceId();
        ProcessorTracker processorTracker = ProcessorTrackerPool.getProcessorTracker(instanceId);
        if (processorTracker == null) {
            log.warn("[ProcessorTrackerActor] ProcessorTracker for instance(instanceId={}) already destroyed.", instanceId);
        }else {
            processorTracker.destroy();
        }
    }
}
