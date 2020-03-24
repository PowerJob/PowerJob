package com.github.kfcfans.oms.worker.core.tracker.task;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import com.github.kfcfans.common.JobInstanceStatus;
import com.github.kfcfans.common.request.TaskTrackerReportInstanceStatusReq;
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
import com.github.kfcfans.oms.worker.pojo.request.ProcessorReportTaskStatusReq;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 负责管理 JobInstance 的运行，主要包括任务的派发（MR可能存在大量的任务）和状态的更新
 *
 * @author tjq
 * @since 2020/3/17
 */
@Slf4j
public abstract class TaskTracker {

    protected long startTime;
    protected long jobTimeLimitMS;

    // 任务实例信息
    protected JobInstanceInfo jobInstanceInfo;
    protected ActorRef taskTrackerActorRef;

    @Getter
    protected List<String> allWorkerAddress;

    protected TaskPersistenceService taskPersistenceService;
    protected ScheduledExecutorService scheduledPool;

    protected AtomicBoolean finished = new AtomicBoolean(false);

    public TaskTracker(JobInstanceInfo jobInstanceInfo, ActorRef taskTrackerActorRef) {

        this.startTime = System.currentTimeMillis();
        this.jobTimeLimitMS = jobInstanceInfo.getTimeLimit();

        this.jobInstanceInfo = jobInstanceInfo;
        this.taskTrackerActorRef = taskTrackerActorRef;
        this.taskPersistenceService = TaskPersistenceService.INSTANCE;

        ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("oms-TaskTrackerTimingPool-%d").build();
        this.scheduledPool = Executors.newScheduledThreadPool(2, factory);

        allWorkerAddress = CommonSJ.commaSplitter.splitToList(jobInstanceInfo.getAllWorkerAddress());

        // 持久化根任务
        persistenceRootTask();

        // 定时任务1：任务派发
        scheduledPool.scheduleWithFixedDelay(new DispatcherRunnable(), 0, 5, TimeUnit.SECONDS);

        // 定时任务2：状态检查
        scheduledPool.scheduleWithFixedDelay(new StatusCheckRunnable(), 10, 10, TimeUnit.SECONDS);
    }


    /**
     * 更新任务状态
     */
    public void updateTaskStatus(String instanceId, String taskId, int status,@Nullable String result) {
        TaskStatus taskStatus = TaskStatus.of(status);

        // 持久化，失败则重试一次（本地数据库操作几乎可以认为可靠...吧...）
        boolean updateResult = taskPersistenceService.updateTaskStatus(instanceId, taskId, taskStatus, result);
        if (!updateResult) {
            try {
                Thread.sleep(100);
                taskPersistenceService.updateTaskStatus(instanceId, taskId, taskStatus, result);
            }catch (Exception ignore) {
            }
        }
        if (!updateResult) {
            log.warn("[TaskTracker] update task status failed, this task(instanceId={}&taskId={}) may be processed repeatedly!", instanceId, taskId);
        }
    }

    /**
     * 新增任务，上层保证 batchSize
     * @param newTaskList 新增的子任务列表
     */
    public boolean addTask(List<TaskDO> newTaskList) {

        if (CollectionUtils.isEmpty(newTaskList)) {
            return true;
        }

        // 基础处理（多循环一次虽然有些浪费，但分布式执行中，这点耗时绝不是主要占比，忽略不计！）
        newTaskList.forEach(task -> {
            task.setJobId(jobInstanceInfo.getJobId());
            task.setInstanceId(jobInstanceInfo.getInstanceId());
            task.setStatus(TaskStatus.WAITING_DISPATCH.getValue());
            task.setFailedCnt(0);
            task.setLastModifiedTime(System.currentTimeMillis());
            task.setCreatedTime(System.currentTimeMillis());
        });

        log.debug("[TaskTracker] JobInstance(id={}) add tasks: {}", jobInstanceInfo.getInstanceId(), newTaskList);
        return taskPersistenceService.batchSave(newTaskList);
    }

    public boolean finished() {
        return finished.get();
    }

    /**
     * 任务是否超时
     */
    public boolean isTimeout() {
        return System.currentTimeMillis() - startTime > jobTimeLimitMS;
    }

    /**
     * 持久化根任务，只有完成持久化才能视为任务开始running（先持久化，再报告server）
     */
    private void persistenceRootTask() {

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

        if (!taskPersistenceService.save(rootTask)) {
            throw new RuntimeException("create root task failed.");
        }
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
                    String targetPath = AkkaUtils.getAkkaRemotePath(targetIP, AkkaConstant.PROCESSOR_TRACKER_ACTOR_NAME);
                    ActorSelection targetActor = OhMyWorker.actorSystem.actorSelection(targetPath);

                    // 发送请求（Akka的tell是至少投递一次，经实验表明无法投递消息也不会报错...印度啊...）
                    targetActor.tell(req, taskTrackerActorRef);

                    // 更新数据库（如果更新数据库失败，可能导致重复执行，先不处理）
                    taskPersistenceService.updateTaskStatus(task.getInstanceId(), task.getTaskId(), TaskStatus.DISPATCH_SUCCESS_WORKER_UNCHECK, null);
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

            // 1. 查询统计信息
            Map<TaskStatus, Long> status2Num = taskPersistenceService.getTaskStatusStatistics(jobInstanceInfo.getInstanceId());

            long waitingDispatchNum = status2Num.get(TaskStatus.WAITING_DISPATCH);
            long workerUnreceivedNum = status2Num.get(TaskStatus.DISPATCH_SUCCESS_WORKER_UNCHECK);
            long receivedNum = status2Num.get(TaskStatus.RECEIVE_SUCCESS);
            long succeedNum = status2Num.get(TaskStatus.WORKER_PROCESS_SUCCESS);
            long failedNum = status2Num.get(TaskStatus.WORKER_PROCESS_FAILED);

            long finishedNum = succeedNum + failedNum;
            long unfinishedNum = waitingDispatchNum + workerUnreceivedNum + receivedNum;

            log.debug("[TaskTracker] status check result({})", status2Num);

            TaskTrackerReportInstanceStatusReq req = new TaskTrackerReportInstanceStatusReq();
            req.setTotalTaskNum(finishedNum + unfinishedNum);
            req.setSucceedTaskNum(succeedNum);
            req.setFailedTaskNum(failedNum);

            // 2. 如果未完成任务数为0，上报服务器
            if (unfinishedNum == 0) {
                finished.set(true);

                if (failedNum == 0) {
                    req.setInstanceStatus(JobInstanceStatus.SUCCEED.getValue());
                }else {
                    req.setInstanceStatus(JobInstanceStatus.FAILED.getValue());
                }

                // 特殊处理MapReduce任务(执行reduce)
                // 特殊处理广播任务任务（执行postProcess）

                // 通知 ProcessorTracker 释放资源（PT释放资源前）
            }else {

            }

        }
    }
}
