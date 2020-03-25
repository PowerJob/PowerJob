package com.github.kfcfans.oms.worker.actors;

import akka.actor.AbstractActor;
import com.github.kfcfans.common.request.ServerScheduleJobReq;
import com.github.kfcfans.oms.worker.common.constants.TaskConstant;
import com.github.kfcfans.oms.worker.common.constants.TaskStatus;
import com.github.kfcfans.oms.worker.core.tracker.task.TaskTracker;
import com.github.kfcfans.oms.worker.core.tracker.task.TaskTrackerPool;
import com.github.kfcfans.oms.worker.persistence.TaskDO;
import com.github.kfcfans.oms.worker.pojo.model.JobInstanceInfo;
import com.github.kfcfans.oms.worker.pojo.request.BroadcastTaskPreExecuteFinishedReq;
import com.github.kfcfans.oms.worker.pojo.request.ProcessorMapTaskRequest;
import com.github.kfcfans.oms.worker.pojo.request.ProcessorReportTaskStatusReq;
import com.github.kfcfans.common.response.AskResponse;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

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

            // 状态转化
            TaskStatus status = TaskStatus.convertStatus(TaskStatus.of(req.getStatus()));

            taskTracker.updateTaskStatus(req.getInstanceId(), req.getTaskId(), status.getValue(), req.getResult(), false);
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
            });

            success = taskTracker.addTask(subTaskList);
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

        log.info("[TaskTrackerActor] instance(id={}) pre process finished.", req.getInstanceId());

        // 1. 生成集群子任务
        boolean success = req.isSuccess();
        if (success) {
            List<String> allWorkerAddress = taskTracker.getAllWorkerAddress();
            List<TaskDO> subTaskList = Lists.newLinkedList();
            for (int i = 0; i < allWorkerAddress.size(); i++) {
                TaskDO subTask = new TaskDO();
                subTask.setTaskName(TaskConstant.BROADCAST_TASK_NAME);
                subTask.setTaskId(TaskConstant.ROOT_TASK_ID + "." + i);

                subTaskList.add(subTask);
            }
            taskTracker.addTask(subTaskList);
        }else {
            log.info("[TaskTrackerActor] BroadcastTask(instanceId={}) failed because of preProcess failed.", req.getInstanceId());
        }

        // 2. 更新根任务状态（广播任务的根任务为 preProcess 任务）
        int status = success ? TaskStatus.WORKER_PROCESS_SUCCESS.getValue() : TaskStatus.WORKER_PROCESS_FAILED.getValue();
        taskTracker.updateTaskStatus(req.getInstanceId(), req.getTaskId(), status, req.getMsg(), false);
    }

    /**
     * 服务器任务调度处理器
     */
    private void onReceiveServerScheduleJobReq(ServerScheduleJobReq req) {
        String instanceId = req.getInstanceId();
        TaskTracker taskTracker = TaskTrackerPool.getTaskTrackerPool(instanceId);

        if (taskTracker != null) {
            log.warn("[TaskTrackerActor] TaskTracker({}) for instance(id={}) already exists.", taskTracker, instanceId);
            return;
        }

        // 原子创建，防止多实例的存在
        TaskTrackerPool.atomicCreateTaskTracker(instanceId, ignore -> {

            JobInstanceInfo jobInstanceInfo = new JobInstanceInfo();
            BeanUtils.copyProperties(req, jobInstanceInfo);

            return new TaskTracker(jobInstanceInfo);
        });
    }
}
