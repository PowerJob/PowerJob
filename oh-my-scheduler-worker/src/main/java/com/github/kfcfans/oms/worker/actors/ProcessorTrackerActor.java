package com.github.kfcfans.oms.worker.actors;

import akka.actor.AbstractActor;
import com.github.kfcfans.oms.worker.core.tracker.processor.ProcessorTracker;
import com.github.kfcfans.oms.worker.core.tracker.processor.ProcessorTrackerPool;
import com.github.kfcfans.oms.worker.persistence.TaskDO;
import com.github.kfcfans.oms.worker.pojo.request.TaskTrackerStartTaskReq;
import com.github.kfcfans.oms.worker.pojo.request.TaskTrackerStopInstanceReq;
import lombok.extern.slf4j.Slf4j;

/**
 * 普通计算节点，处理来自 TaskTracker 的请求
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

        Long instanceId = req.getInstanceInfo().getInstanceId();

        // 创建 ProcessorTracker 一定能成功，且每个任务实例只会创建一个 ProcessorTracker
        ProcessorTracker processorTracker = ProcessorTrackerPool.getProcessorTracker(instanceId, ignore -> new ProcessorTracker(req));

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
