package com.github.kfcfans.oms.worker.tracker;

import akka.actor.ActorRef;
import com.github.kfcfans.common.ExecuteType;
import com.github.kfcfans.oms.worker.common.constants.CommonSJ;
import com.github.kfcfans.oms.worker.common.constants.TaskConstant;
import com.github.kfcfans.oms.worker.common.utils.NetUtils;
import com.github.kfcfans.oms.worker.persistence.TaskDO;
import com.github.kfcfans.oms.worker.persistence.TaskPersistenceService;
import com.github.kfcfans.oms.worker.pojo.model.JobInstanceInfo;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * 负责管理 JobInstance 的运行，主要包括任务的派发（MR可能存在大量的任务）和状态的更新
 *
 * @author tjq
 * @since 2020/3/17
 */
public abstract class TaskTracker {

    // 任务实例信息
    protected JobInstanceInfo jobInstanceInfo;
    protected ActorRef actor;

    protected TaskPersistenceService taskPersistenceService;


    /**
     * 分发任务
     */
    public abstract void dispatch();

    public void updateTaskStatus() {
    }

    public boolean finished() {
        return false;
    }

    /**
     * 持久化根任务，只有完成持久化才能视为任务开始running（先持久化，再报告server）
     */
    private void persistenceTask() {

        ExecuteType executeType = ExecuteType.valueOf(jobInstanceInfo.getExecuteType());
        boolean persistenceResult;

        // 单机、MR模型下，根任务模型本机直接执行（JobTracker一般为负载最小的机器，且MR的根任务通常伴随着 map 操作，本机执行可以有效减少网络I/O开销）
        if (executeType != ExecuteType.BROADCAST) {
            TaskDO rootTask = new TaskDO();
            rootTask.setStatus(1);
            rootTask.setJobId(jobInstanceInfo.getJobId());
            rootTask.setInstanceId(jobInstanceInfo.getInstanceId());
            rootTask.setTaskId(TaskConstant.ROOT_TASK_ID);
            rootTask.setAddress(NetUtils.getLocalHost());
            rootTask.setTaskName(TaskConstant.ROOT_TASK_NAME);
            rootTask.setCreatedTime(System.currentTimeMillis());
            rootTask.setCreatedTime(System.currentTimeMillis());

            persistenceResult = taskPersistenceService.save(rootTask);
        }else {
            List<TaskDO> taskList = Lists.newLinkedList();
            List<String> addrList = CommonSJ.commaSplitter.splitToList(jobInstanceInfo.getWorkerAddress());
            for (int i = 0; i < addrList.size(); i++) {
                TaskDO task = new TaskDO();
                task.setStatus(1);
                task.setJobId(jobInstanceInfo.getJobId());
                task.setInstanceId(jobInstanceInfo.getInstanceId());
                task.setTaskId(String.valueOf(i));
                task.setAddress(addrList.get(i));
                task.setTaskName(TaskConstant.ROOT_TASK_NAME);
                task.setCreatedTime(System.currentTimeMillis());
                task.setCreatedTime(System.currentTimeMillis());

                taskList.add(task);
            }
            persistenceResult = taskPersistenceService.batchSave(taskList);
        }

        if (!persistenceResult) {
            throw new RuntimeException("create root task failed.");
        }
    }

    /**
     * 启动任务分发器
     */
    private void initDispatcher() {

    }
}
