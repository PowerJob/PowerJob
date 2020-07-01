package com.github.kfcfans.powerjob.worker.actors;

import akka.actor.AbstractActor;
import com.github.kfcfans.powerjob.worker.core.tracker.processor.ProcessorTracker;
import com.github.kfcfans.powerjob.worker.core.tracker.processor.ProcessorTrackerPool;
import com.github.kfcfans.powerjob.worker.persistence.TaskDO;
import com.github.kfcfans.powerjob.worker.pojo.request.TaskTrackerStartTaskReq;
import com.github.kfcfans.powerjob.worker.pojo.request.TaskTrackerStopInstanceReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.List;

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
     * @param req 请求
     */
    private void onReceiveTaskTrackerStartTaskReq(TaskTrackerStartTaskReq req) {

        Long instanceId = req.getInstanceInfo().getInstanceId();

        // 创建 ProcessorTracker 一定能成功
        ProcessorTracker processorTracker = ProcessorTrackerPool.getProcessorTracker(instanceId, req.getTaskTrackerAddress(), () -> new ProcessorTracker(req));

        TaskDO task = new TaskDO();

        task.setTaskId(req.getTaskId());
        task.setTaskName(req.getTaskName());
        task.setTaskContent(req.getTaskContent());
        task.setFailedCnt(req.getTaskCurrentRetryNums());
        task.setSubInstanceId(req.getSubInstanceId());

        processorTracker.submitTask(task);
    }

    /**
     * 处理来自TaskTracker停止任务的请求
     * @param req 请求
     */
    private void onReceiveTaskTrackerStopInstanceReq(TaskTrackerStopInstanceReq req) {

        Long instanceId = req.getInstanceId();
        List<ProcessorTracker> removedPts = ProcessorTrackerPool.removeProcessorTracker(instanceId);
        if (!CollectionUtils.isEmpty(removedPts)) {
            removedPts.forEach(ProcessorTracker::destroy);
        }
    }
}
