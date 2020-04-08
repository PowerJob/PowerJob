package com.github.kfcfans.oms.worker.actors;

import akka.actor.AbstractActor;
import com.github.kfcfans.common.request.ServerScheduleJobReq;
import com.github.kfcfans.oms.worker.core.tracker.task.CommonTaskTracker;
import com.github.kfcfans.oms.worker.core.tracker.task.TaskTracker;
import com.github.kfcfans.oms.worker.core.tracker.task.TaskTrackerPool;
import com.github.kfcfans.oms.worker.persistence.TaskDO;
import com.github.kfcfans.oms.worker.pojo.request.BroadcastTaskPreExecuteFinishedReq;
import com.github.kfcfans.oms.worker.pojo.request.ProcessorMapTaskRequest;
import com.github.kfcfans.oms.worker.pojo.request.ProcessorReportTaskStatusReq;
import com.github.kfcfans.common.response.AskResponse;
import com.github.kfcfans.oms.worker.pojo.request.ProcessorTrackerStatusReportReq;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * worker的master节点，处理来自server的jobInstance请求和来自worker的task请求
 *
 * @author tjq
 * @since 2020/3/17
 */
@Slf4j
public class TaskTrackerActor extends AbstractActor {

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ProcessorReportTaskStatusReq.class, this::onReceiveProcessorReportTaskStatusReq)
                .match(ServerScheduleJobReq.class, this::onReceiveServerScheduleJobReq)
                .match(ProcessorMapTaskRequest.class, this::onReceiveProcessorMapTaskRequest)
                .match(ProcessorTrackerStatusReportReq.class, this::onReceiveProcessorTrackerStatusReportReq)
                .match(BroadcastTaskPreExecuteFinishedReq.class, this::onReceiveBroadcastTaskPreExecuteFinishedReq)
                .matchAny(obj -> log.warn("[ServerRequestActor] receive unknown request: {}.", obj))
                .build();
    }


    /**
     * 子任务状态上报 处理器
     */
    private void onReceiveProcessorReportTaskStatusReq(ProcessorReportTaskStatusReq req) {

        TaskTracker taskTracker = TaskTrackerPool.getTaskTrackerPool(req.getInstanceId());
        if (taskTracker == null) {
            log.warn("[TaskTrackerActor] receive ProcessorReportTaskStatusReq({}) but system can't find TaskTracker.", req);
        } else {

            taskTracker.updateTaskStatus(req.getTaskId(), req.getStatus(), req.getResult());
        }
    }

    /**
     * 子任务 map 处理器
     */
    private void onReceiveProcessorMapTaskRequest(ProcessorMapTaskRequest req) {

        TaskTracker taskTracker = TaskTrackerPool.getTaskTrackerPool(req.getInstanceId());
        if (taskTracker == null) {
            log.warn("[TaskTrackerActor] receive ProcessorMapTaskRequest({}) but system can't find TaskTracker.", req);
            return;
        }

        boolean success = false;
        List<TaskDO> subTaskList = Lists.newLinkedList();

        try {

            req.getSubTasks().forEach(originSubTask -> {
                TaskDO subTask = new TaskDO();
                subTask.setTaskName(req.getTaskName());
                subTask.setTaskId(originSubTask.getTaskId());
                subTask.setTaskContent(originSubTask.getTaskContent());

                subTaskList.add(subTask);
            });

            success = taskTracker.submitTask(subTaskList);
        }catch (Exception e) {
            log.warn("[TaskTrackerActor] process map task(instanceId={}) failed.", req.getInstanceId(), e);
        }

        AskResponse response = new AskResponse(success);
        getSender().tell(response, getSelf());
    }

    /**
     * 广播任务前置任务执行完毕 处理器
     */
    private void onReceiveBroadcastTaskPreExecuteFinishedReq(BroadcastTaskPreExecuteFinishedReq req) {

        TaskTracker taskTracker = TaskTrackerPool.getTaskTrackerPool(req.getInstanceId());
        if (taskTracker == null) {
            log.warn("[TaskTrackerActor] receive BroadcastTaskPreExecuteFinishedReq({}) but system can't find TaskTracker.", req);
            return;
        }
        taskTracker.broadcast(req.isSuccess(), req.getTaskId(), req.getMsg());
    }

    /**
     * 服务器任务调度处理器
     */
    private void onReceiveServerScheduleJobReq(ServerScheduleJobReq req) {
        Long instanceId = req.getInstanceId();
        TaskTracker taskTracker = TaskTrackerPool.getTaskTrackerPool(instanceId);

        if (taskTracker != null) {
            log.warn("[TaskTrackerActor] TaskTracker({}) for instance(id={}) already exists.", taskTracker, instanceId);
            return;
        }

        // 原子创建，防止多实例的存在
        TaskTrackerPool.atomicCreateTaskTracker(instanceId, ignore -> new CommonTaskTracker(req));
    }

    /**
     * ProcessorTracker 心跳处理器
     */
    private void onReceiveProcessorTrackerStatusReportReq(ProcessorTrackerStatusReportReq req) {
        TaskTracker taskTracker = TaskTrackerPool.getTaskTrackerPool(req.getInstanceId());
        if (taskTracker == null) {
            log.warn("[TaskTrackerActor] receive ProcessorTrackerStatusReportReq({}) but system can't find TaskTracker.", req);
            return;
        }
        taskTracker.receiveProcessorTrackerHeartbeat(req);
    }
}
