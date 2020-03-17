package com.github.kfcfans.oms.worker.tracker;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import com.github.kfcfans.common.ExecuteType;
import com.github.kfcfans.oms.worker.OhMyWorker;
import com.github.kfcfans.oms.worker.common.constants.AkkaConstant;
import com.github.kfcfans.oms.worker.common.constants.CommonSJ;
import com.github.kfcfans.oms.worker.common.constants.TaskConstant;
import com.github.kfcfans.oms.worker.common.constants.TaskStatus;
import com.github.kfcfans.oms.worker.common.utils.AkkaUtils;
import com.github.kfcfans.oms.worker.common.utils.NetUtils;
import com.github.kfcfans.oms.worker.persistence.TaskDO;
import com.github.kfcfans.oms.worker.persistence.TaskPersistenceService;
import com.github.kfcfans.oms.worker.pojo.model.JobInstanceInfo;
import com.github.kfcfans.oms.worker.pojo.request.TaskTrackerStartTaskReq;
import com.github.kfcfans.oms.worker.pojo.request.WorkerReportTaskStatusReq;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 负责管理 JobInstance 的运行，主要包括任务的派发（MR可能存在大量的任务）和状态的更新
 *
 * @author tjq
 * @since 2020/3/17
 */
@Slf4j
public abstract class TaskTracker {

    // 任务实例信息
    protected JobInstanceInfo jobInstanceInfo;
    protected ActorRef taskTrackerActorRef;

    protected List<String> allWorkerAddress;

    protected TaskPersistenceService taskPersistenceService;
    protected ScheduledExecutorService scheduledPool;

    // 统计
    protected AtomicBoolean finished = new AtomicBoolean(false);
    protected AtomicLong needDispatchTaskNum = new AtomicLong(0);
    protected AtomicLong dispatchedTaskNum = new AtomicLong(0);
    protected AtomicLong waitingToRunTaskNum = new AtomicLong(0);
    protected AtomicLong runningTaskNum = new AtomicLong(0);
    protected AtomicLong successTaskNum = new AtomicLong(0);
    protected AtomicLong failedTaskNum = new AtomicLong(0);

    public TaskTracker(JobInstanceInfo jobInstanceInfo, ActorRef taskTrackerActorRef) {

        this.jobInstanceInfo = jobInstanceInfo;
        this.taskTrackerActorRef = taskTrackerActorRef;
        this.taskPersistenceService = TaskPersistenceService.INSTANCE;

        ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("TaskTrackerTimingPool-%s").build();
        this.scheduledPool = Executors.newScheduledThreadPool(2, factory);

        allWorkerAddress = CommonSJ.commaSplitter.splitToList(jobInstanceInfo.getAllWorkerAddress());
    }


    /**
     * 分发任务
     */
    public abstract void dispatch();

    public void updateTaskStatus(WorkerReportTaskStatusReq statusReportRequest) {
        TaskStatus taskStatus = TaskStatus.of(statusReportRequest.getStatus());
        // 持久化

        // 更新统计数据
        switch (taskStatus) {
            case RECEIVE_SUCCESS:
                waitingToRunTaskNum.incrementAndGet();break;
            case PROCESSING:

        }
    }

    public boolean finished() {
        return finished.get();
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
            rootTask.setStatus(TaskStatus.WAITING_DISPATCH.getValue());
            rootTask.setJobId(jobInstanceInfo.getJobId());
            rootTask.setInstanceId(jobInstanceInfo.getInstanceId());
            rootTask.setTaskId(TaskConstant.ROOT_TASK_ID);
            rootTask.setFailedCnt(0);
            rootTask.setAddress(NetUtils.getLocalHost());
            rootTask.setTaskName(TaskConstant.ROOT_TASK_NAME);
            rootTask.setCreatedTime(System.currentTimeMillis());
            rootTask.setCreatedTime(System.currentTimeMillis());

            persistenceResult = taskPersistenceService.save(rootTask);
            needDispatchTaskNum.incrementAndGet();
        }else {
            List<TaskDO> taskList = Lists.newLinkedList();
            List<String> addrList = CommonSJ.commaSplitter.splitToList(jobInstanceInfo.getAllWorkerAddress());
            for (int i = 0; i < addrList.size(); i++) {
                TaskDO task = new TaskDO();
                task.setStatus(TaskStatus.WAITING_DISPATCH.getValue());
                task.setJobId(jobInstanceInfo.getJobId());
                task.setInstanceId(jobInstanceInfo.getInstanceId());
                task.setTaskId(String.valueOf(i));
                task.setAddress(addrList.get(i));
                task.setFailedCnt(0);
                task.setTaskName(TaskConstant.ROOT_TASK_NAME);
                task.setCreatedTime(System.currentTimeMillis());
                task.setCreatedTime(System.currentTimeMillis());

                taskList.add(task);
            }
            persistenceResult = taskPersistenceService.batchSave(taskList);
            needDispatchTaskNum.addAndGet(taskList.size());
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

    public void destroy() {
        scheduledPool.shutdown();
    }

    /**
     * 定时扫描数据库中的task（出于内存占用量考虑，每次最多获取100个），并将需要执行的任务派发出去
     */
    private class DispatcherRunnable implements Runnable {

        @Override
        public void run() {
            taskPersistenceService.getNeedDispatchTask(jobInstanceInfo.getInstanceId()).forEach(task -> {
                try {
                    // 构造 worker 执行请求
                    TaskTrackerStartTaskReq req = new TaskTrackerStartTaskReq(jobInstanceInfo, task);

                    // 构造 akka 可访问节点路径
                    String targetIP = task.getAddress();
                    if (StringUtils.isEmpty(targetIP)) {
                        targetIP = allWorkerAddress.get(ThreadLocalRandom.current().nextInt(allWorkerAddress.size()));
                    }
                    String targetPath = AkkaUtils.getAkkaRemotePath(targetIP, AkkaConstant.WORKER_ACTOR_NAME);
                    ActorSelection targetActor = OhMyWorker.actorSystem.actorSelection(targetPath);

                    // 发送请求（Akka的tell是至少投递一次，经实验表明无法投递消息也不会报错...印度啊...）
                    targetActor.tell(req, taskTrackerActorRef);

                    // 更新数据库（如果更新数据库失败，可能导致重复执行，先不处理）
                    taskPersistenceService.updateTaskStatus(task.getInstanceId(), task.getTaskId(), TaskStatus.DISPATCH_SUCCESS);

                    // 更新统计数据
                    needDispatchTaskNum.decrementAndGet();
                    dispatchedTaskNum.incrementAndGet();

                }catch (Exception e) {
                    // 调度失败，不修改数据库，下次重新随机派发给 remote actor
                    log.warn("[TaskTracker] dispatch task({}) failed.", task);
                }
            });
        }
    }

    /**
     * 定时检查当前任务的执行状态
     */
    private class StatusCheckRunnable implements Runnable {

        @Override
        public void run() {

        }
    }
}
