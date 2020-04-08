package com.github.kfcfans.oms.worker.core.tracker.task;

import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import com.github.kfcfans.common.ExecuteType;
import com.github.kfcfans.common.InstanceStatus;
import com.github.kfcfans.common.request.ServerScheduleJobReq;
import com.github.kfcfans.common.request.TaskTrackerReportInstanceStatusReq;
import com.github.kfcfans.common.response.AskResponse;
import com.github.kfcfans.common.utils.CommonUtils;
import com.github.kfcfans.oms.worker.OhMyWorker;
import com.github.kfcfans.common.RemoteConstant;
import com.github.kfcfans.oms.worker.common.constants.TaskConstant;
import com.github.kfcfans.oms.worker.common.constants.TaskStatus;
import com.github.kfcfans.oms.worker.common.utils.AkkaUtils;
import com.github.kfcfans.common.utils.NetUtils;
import com.github.kfcfans.oms.worker.core.ha.ProcessorTrackerStatusHolder;
import com.github.kfcfans.oms.worker.persistence.TaskDO;
import com.github.kfcfans.oms.worker.persistence.TaskPersistenceService;
import com.github.kfcfans.oms.worker.pojo.model.InstanceInfo;
import com.github.kfcfans.oms.worker.pojo.request.ProcessorTrackerStatusReportReq;
import com.github.kfcfans.oms.worker.pojo.request.TaskTrackerStartTaskReq;
import com.github.kfcfans.oms.worker.pojo.request.TaskTrackerStopInstanceReq;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 负责管理 JobInstance 的运行，主要包括任务的派发（MR可能存在大量的任务）和状态的更新
 *
 * @author tjq
 * @since 2020/3/17
 */
@Slf4j
@ToString
public class TaskTracker {

    private long startTime;
    // 任务实例信息
    private InstanceInfo instanceInfo;

    @Getter
    private ProcessorTrackerStatusHolder ptStatusHolder;

    // 数据库持久化服务
    private TaskPersistenceService taskPersistenceService;
    // 定时任务线程池
    private ScheduledExecutorService scheduledPool;

    private AtomicBoolean finished = new AtomicBoolean(false);

    public TaskTracker(ServerScheduleJobReq req) {

        this.startTime = System.currentTimeMillis();

        // 1. 初始化成员变量
        this.instanceInfo = new InstanceInfo();
        BeanUtils.copyProperties(req, instanceInfo);
        this.ptStatusHolder = new ProcessorTrackerStatusHolder(req.getAllWorkerAddress());
        this.taskPersistenceService = TaskPersistenceService.INSTANCE;

        ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("oms-TaskTrackerTimingPool-%d").build();
        this.scheduledPool = Executors.newScheduledThreadPool(2, factory);

        // 2. 持久化根任务
        persistenceRootTask();

        // 3. 启动定时任务（任务派发 & 状态检查）
        scheduledPool.scheduleWithFixedDelay(new DispatcherRunnable(), 0, 1, TimeUnit.SECONDS);
        scheduledPool.scheduleWithFixedDelay(new StatusCheckRunnable(), 10, 10, TimeUnit.SECONDS);

        log.info("[TaskTracker-{}] create TaskTracker from request({}) successfully.", req.getInstanceId(), req);
    }


    /**
     * 更新任务状态
     * 任务状态机只允许数字递增
     */
    public void updateTaskStatus(Long instanceId, String taskId, int newStatus, @Nullable String result) {

        boolean updateResult;
        TaskStatus nTaskStatus = TaskStatus.of(newStatus);
        // 1. 读取当前 Task 状态，防止逆状态机变更的出现
        Optional<TaskStatus> dbTaskStatusOpt = taskPersistenceService.getTaskStatus(instanceId, taskId);

        if (!dbTaskStatusOpt.isPresent()) {
            log.warn("[TaskTracker-{}] query TaskStatus from DB failed when try to update new TaskStatus(taskId={},newStatus={}).",
                    instanceId, taskId, newStatus);
        }

        // 2. 数据库没查到，也允许写入（这个还需要日后仔细考虑）
        if (dbTaskStatusOpt.orElse(TaskStatus.WAITING_DISPATCH).getValue() > newStatus) {
            // 必存在，但不怎么写，Java会警告...
            TaskStatus dbTaskStatus = dbTaskStatusOpt.orElse(TaskStatus.WAITING_DISPATCH);
            log.warn("[TaskTracker-{}] task(taskId={},dbStatus={},requestStatus={}) status conflict, TaskTracker won't update the status.",
                    instanceId, taskId, dbTaskStatus, nTaskStatus);
            return;
        }

        // 3. 失败重试处理
        if (nTaskStatus == TaskStatus.WORKER_PROCESS_FAILED) {

            // 数据库查询失败的话，就只重试一次
            int failedCnt = taskPersistenceService.getTaskFailedCnt(instanceId, taskId).orElse(instanceInfo.getTaskRetryNum() - 1);
            if (failedCnt < instanceInfo.getTaskRetryNum()) {

                TaskDO updateEntity = new TaskDO();
                updateEntity.setFailedCnt(failedCnt + 1);
                updateEntity.setAddress(RemoteConstant.EMPTY_ADDRESS);
                updateEntity.setStatus(TaskStatus.WAITING_DISPATCH.getValue());

                boolean retryTask = taskPersistenceService.updateTask(instanceId, taskId, updateEntity);
                if (retryTask) {
                    log.info("[TaskTracker-{}] task(taskId={}) process failed, TaskTracker will have a retry.", instanceId, taskId);
                    return;
                }
            }
        }

        // 4. 更新状态（失败重试写入DB失败的，也就不重试了...谁让你那么倒霉呢...）
        TaskDO updateEntity = new TaskDO();
        updateEntity.setStatus(nTaskStatus.getValue());
        updateEntity.setResult(result);
        updateResult = taskPersistenceService.updateTask(instanceId, taskId, updateEntity);

        if (!updateResult) {
            log.warn("[TaskTracker-{}] update task status failed, this task(taskId={}) may be processed repeatedly!", instanceId, taskId);
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
            task.setJobId(instanceInfo.getJobId());
            task.setInstanceId(instanceInfo.getInstanceId());
            task.setStatus(TaskStatus.WAITING_DISPATCH.getValue());
            task.setFailedCnt(0);
            task.setLastModifiedTime(System.currentTimeMillis());
            task.setCreatedTime(System.currentTimeMillis());
        });

        log.debug("[TaskTracker-{}] receive new tasks: {}", instanceInfo.getInstanceId(), newTaskList);
        return taskPersistenceService.batchSave(newTaskList);
    }

    /**
     * ProcessorTracker 上报健康状态
     */
    public void receiveProcessorTrackerHeartbeat(ProcessorTrackerStatusReportReq heartbeatReq) {
        ptStatusHolder.updateStatus(heartbeatReq);
        log.debug("[TaskTracker-{}] receive heartbeat: {}", instanceInfo.getInstanceId(), heartbeatReq);
    }

    public boolean finished() {
        return finished.get();
    }

    /**
     * 任务是否超时
     */
    public boolean isTimeout() {
        return System.currentTimeMillis() - startTime > instanceInfo.getInstanceTimeoutMS();
    }

    /**
     * 持久化根任务，只有完成持久化才能视为任务开始running（先持久化，再报告server）
     */
    private void persistenceRootTask() {

        TaskDO rootTask = new TaskDO();
        rootTask.setStatus(TaskStatus.WAITING_DISPATCH.getValue());
        rootTask.setJobId(instanceInfo.getJobId());
        rootTask.setInstanceId(instanceInfo.getInstanceId());
        rootTask.setTaskId(TaskConstant.ROOT_TASK_ID);
        rootTask.setFailedCnt(0);
        rootTask.setAddress(OhMyWorker.getWorkerAddress());
        rootTask.setTaskName(TaskConstant.ROOT_TASK_NAME);
        rootTask.setCreatedTime(System.currentTimeMillis());
        rootTask.setLastModifiedTime(System.currentTimeMillis());

        if (!taskPersistenceService.save(rootTask)) {
            log.error("[TaskTracker-{}] create root task failed.", instanceInfo.getInstanceId());
        }else {
            log.info("[TaskTracker-{}] create root task successfully.", instanceInfo.getInstanceId());
        }
    }


    public void destroy() {

        // 0. 先关闭定时任务线程池，防止任务被派发出去
        CommonUtils.executeIgnoreException(() -> {
            // 不能使用 shutdownNow()，因为 destroy 方法本身就在 scheduledPool 的线程中执行，强行关闭会打断 destroy 的执行。
            scheduledPool.shutdown();
            return null;
        });

        // 1. 通知 ProcessorTracker 释放资源
        Long instanceId = instanceInfo.getInstanceId();
        TaskTrackerStopInstanceReq stopRequest = new TaskTrackerStopInstanceReq();
        stopRequest.setInstanceId(instanceId);
        ptStatusHolder.getAllProcessorTrackers().forEach(ptIP -> {
            String ptPath = AkkaUtils.getAkkaWorkerPath(ptIP, RemoteConstant.PROCESSOR_TRACKER_ACTOR_NAME);
            ActorSelection ptActor = OhMyWorker.actorSystem.actorSelection(ptPath);
            // 不可靠通知，ProcessorTracker 也可以靠自己的定时任务/问询等方式关闭
            ptActor.tell(stopRequest, null);
        });

        // 2. 删除所有数据库数据
        boolean dbSuccess = taskPersistenceService.deleteAllTasks(instanceId);
        if (!dbSuccess) {
            log.warn("[TaskTracker-{}] delete tasks from database failed.", instanceId);
        }else {
            log.debug("[TaskTracker-{}] delete all tasks from database successfully.", instanceId);
        }

        // 3. 移除顶层引用，送去 GC
        TaskTrackerPool.remove(instanceId);

        log.info("[TaskTracker-{}] TaskTracker has left the world.", instanceId);
    }

    /**
     * 定时扫描数据库中的task（出于内存占用量考虑，每次最多获取100个），并将需要执行的任务派发出去
     */
    private class DispatcherRunnable implements Runnable {

        // 数据库查询限制，每次最多查询几个任务
        private static final int DB_QUERY_LIMIT = 100;

        @Override
        public void run() {

            if (finished()) {
                return;
            }

            Stopwatch stopwatch = Stopwatch.createStarted();
            Long instanceId = instanceInfo.getInstanceId();

            // 1. 获取可以派发任务的 ProcessorTracker
            List<String> availablePtIps = ptStatusHolder.getAvailableProcessorTrackers();

            // 2. 没有可用 ProcessorTracker，本次不派发
            if (availablePtIps.isEmpty()) {
                log.debug("[TaskTracker-{}] no available ProcessorTracker now.", instanceId);
                return;
            }

            // 3. 避免大查询，分批派发任务
            long currentDispatchNum = 0;
            long maxDispatchNum = availablePtIps.size() * instanceInfo.getThreadConcurrency() * 2;
            AtomicInteger index = new AtomicInteger(0);

            // 4. 循环查询数据库，获取需要派发的任务
            while (maxDispatchNum > currentDispatchNum) {

                int dbQueryLimit = Math.min(DB_QUERY_LIMIT, (int) maxDispatchNum);
                List<TaskDO> needDispatchTasks = taskPersistenceService.getTaskByStatus(instanceId, TaskStatus.WAITING_DISPATCH, dbQueryLimit);
                currentDispatchNum += needDispatchTasks.size();

                needDispatchTasks.forEach(task -> {

                    TaskTrackerStartTaskReq startTaskReq = new TaskTrackerStartTaskReq(instanceInfo, task);

                    // 获取 ProcessorTracker 地址，如果 Task 中自带了 Address，则使用该 Address
                    String ptAddress = task.getAddress();
                    if (StringUtils.isEmpty(ptAddress) || RemoteConstant.EMPTY_ADDRESS.equals(ptAddress)) {
                        ptAddress = availablePtIps.get(index.getAndIncrement() % availablePtIps.size());
                    }
                    String ptActorPath = AkkaUtils.getAkkaWorkerPath(ptAddress, RemoteConstant.PROCESSOR_TRACKER_ACTOR_NAME);
                    ActorSelection ptActor = OhMyWorker.actorSystem.actorSelection(ptActorPath);
                    ptActor.tell(startTaskReq, null);

                    // 更新 ProcessorTrackerStatus 状态
                    ptStatusHolder.getProcessorTrackerStatus(ptAddress).setDispatched(true);
                    // 更新数据库（如果更新数据库失败，可能导致重复执行，先不处理）
                    TaskDO updateEntity = new TaskDO();
                    updateEntity.setStatus(TaskStatus.DISPATCH_SUCCESS_WORKER_UNCHECK.getValue());
                    taskPersistenceService.updateTask(instanceId, task.getTaskId(), updateEntity);

                    log.debug("[TaskTracker-{}] dispatch task(taskId={},taskName={}) successfully.", task.getInstanceId(), task.getTaskId(), task.getTaskName());
                });

                // 数量不足 或 查询失败，则终止循环
                if (needDispatchTasks.size() < dbQueryLimit) {
                    log.debug("[TaskTracker-{}] dispatched {} tasks,using time {}.", instanceId, currentDispatchNum, stopwatch);
                    return;
                }
            }

            log.debug("[TaskTracker-{}] dispatched {} tasks,using time {}.", instanceId, currentDispatchNum, stopwatch);
        }
    }

    /**
     * 定时检查当前任务的执行状态
     */
    private class StatusCheckRunnable implements Runnable {

        private static final long TIME_OUT_MS = 5000;

        private void innerRun() {

            Long instanceId = instanceInfo.getInstanceId();

            // 1. 查询统计信息
            Map<TaskStatus, Long> status2Num = taskPersistenceService.getTaskStatusStatistics(instanceId);

            long waitingDispatchNum = status2Num.getOrDefault(TaskStatus.WAITING_DISPATCH, 0L);
            long workerUnreceivedNum = status2Num.getOrDefault(TaskStatus.DISPATCH_SUCCESS_WORKER_UNCHECK, 0L);
            long receivedNum = status2Num.getOrDefault(TaskStatus.WORKER_RECEIVED, 0L);
            long runningNum = status2Num.getOrDefault(TaskStatus.WORKER_PROCESSING, 0L);
            long failedNum = status2Num.getOrDefault(TaskStatus.WORKER_PROCESS_FAILED, 0L);
            long succeedNum = status2Num.getOrDefault(TaskStatus.WORKER_PROCESS_SUCCESS, 0L);

            long finishedNum = succeedNum + failedNum;
            long unfinishedNum = waitingDispatchNum + workerUnreceivedNum + receivedNum + runningNum;

            log.debug("[TaskTracker-{}] status check result: {}", instanceId, status2Num);

            TaskTrackerReportInstanceStatusReq req = new TaskTrackerReportInstanceStatusReq();
            req.setJobId(instanceInfo.getJobId());
            req.setInstanceId(instanceId);
            req.setTotalTaskNum(finishedNum + unfinishedNum);
            req.setSucceedTaskNum(succeedNum);
            req.setFailedTaskNum(failedNum);
            req.setReportTime(System.currentTimeMillis());

            // 2. 如果未完成任务数为0，判断是否真正结束，并获取真正结束任务的执行结果
            TaskDO resultTask = null;
            if (unfinishedNum == 0) {

                boolean finishedBoolean = true;

                // 数据库中一个任务都没有，说明根任务创建失败，该任务实例失败
                if (finishedNum == 0) {
                    resultTask = new TaskDO();
                    resultTask.setStatus(TaskStatus.WORKER_PROCESS_FAILED.getValue());
                    resultTask.setResult("CREATE_ROOT_TASK_FAILED");

                }else {
                    ExecuteType executeType = ExecuteType.valueOf(instanceInfo.getExecuteType());

                    // STANDALONE 只有一个任务，完成即结束
                    if (executeType == ExecuteType.STANDALONE) {

                        List<TaskDO> allTask = taskPersistenceService.getAllTask(instanceId);
                        if (CollectionUtils.isEmpty(allTask) || allTask.size() > 1) {
                            log.warn("[TaskTracker-{}] there must have some bug in TaskTracker.", instanceId);
                        }else {
                            resultTask = allTask.get(0);
                        }

                    } else {

                        // MapReduce 和 Broadcast 任务实例是否完成根据**Last_Task**的执行情况判断
                        Optional<TaskDO> lastTaskOptional = taskPersistenceService.getLastTask(instanceId);
                        if (lastTaskOptional.isPresent()) {

                            // 存在则根据 reduce 任务来判断状态
                            resultTask = lastTaskOptional.get();
                            TaskStatus lastTaskStatus = TaskStatus.of(resultTask.getStatus());
                            finishedBoolean = lastTaskStatus == TaskStatus.WORKER_PROCESS_SUCCESS || lastTaskStatus == TaskStatus.WORKER_PROCESS_FAILED;
                        }else {

                            // 不存在，代表前置任务刚刚执行完毕，需要创建 lastTask，最终任务必须在本机执行！
                            finishedBoolean = false;

                            TaskDO newLastTask = new TaskDO();
                            newLastTask.setTaskName(TaskConstant.LAST_TASK_NAME);
                            newLastTask.setTaskId(TaskConstant.LAST_TASK_ID);
                            newLastTask.setAddress(OhMyWorker.getWorkerAddress());
                            addTask(Lists.newArrayList(newLastTask));
                        }
                    }
                }


                finished.set(finishedBoolean);
            }

            String serverPath = AkkaUtils.getAkkaServerPath(RemoteConstant.SERVER_ACTOR_NAME);
            ActorSelection serverActor = OhMyWorker.actorSystem.actorSelection(serverPath);

            // 3. 执行完毕，报告服务器（第二个判断则是为了取消烦人的编译器警告）
            if (finished.get() && resultTask != null) {

                boolean success = resultTask.getStatus() == TaskStatus.WORKER_PROCESS_SUCCESS.getValue();
                req.setResult(resultTask.getResult());
                req.setInstanceStatus(success ? InstanceStatus.SUCCEED.getV() : InstanceStatus.FAILED.getV());

                CompletionStage<Object> askCS = Patterns.ask(serverActor, req, Duration.ofMillis(TIME_OUT_MS));

                boolean serverAccepted = false;
                try {
                    AskResponse askResponse = (AskResponse) askCS.toCompletableFuture().get(TIME_OUT_MS, TimeUnit.MILLISECONDS);
                    serverAccepted = askResponse.isSuccess();
                }catch (Exception e) {
                    log.warn("[TaskTracker-{}] report finished status failed, result={}.", instanceId, resultTask.getResult());
                }

                // 服务器未接受上报，则等待下次重新上报
                if (!serverAccepted) {
                    return;
                }

                // 服务器已经更新状态，任务已经执行完毕，开始释放所有资源
                log.info("[TaskTracker-{}] instance(jobId={}) process finished,result = {}, start to release resource...",
                        instanceId, instanceInfo.getJobId(), resultTask.getResult());

                destroy();
                return;
            }

            // 4. 未完成，上报状态
            req.setInstanceStatus(InstanceStatus.RUNNING.getV());
            serverActor.tell(req, null);

            // 5.1 定期检查 -> 重试派发后未确认的任务
            long currentMS = System.currentTimeMillis();
            if (workerUnreceivedNum != 0) {
                taskPersistenceService.getTaskByStatus(instanceId, TaskStatus.DISPATCH_SUCCESS_WORKER_UNCHECK, 100).forEach(uncheckTask -> {

                    long elapsedTime = currentMS - uncheckTask.getLastModifiedTime();
                    if (elapsedTime > TIME_OUT_MS) {

                        TaskDO updateEntity = new TaskDO();
                        updateEntity.setStatus(TaskStatus.WAITING_DISPATCH.getValue());
                        // 特殊任务只能本机执行
                        if (!TaskConstant.LAST_TASK_ID.equals(uncheckTask.getTaskId())) {
                            updateEntity.setAddress(RemoteConstant.EMPTY_ADDRESS);
                        }

                        taskPersistenceService.updateTask(instanceId, uncheckTask.getTaskId(), updateEntity);

                        log.warn("[TaskTracker-{}] task(taskId={}) try to dispatch again due to unreceived the response from ProcessorTracker.",
                                instanceId, uncheckTask.getTaskId());
                    }

                });
            }

            // 5.2 定期检查 -> 重新执行被派发到宕机ProcessorTracker上的任务
            List<String> disconnectedPTs = ptStatusHolder.getAllDisconnectedProcessorTrackers();
            if (!disconnectedPTs.isEmpty()) {
                log.warn("[TaskTracker-{}] some ProcessorTracker disconnected from TaskTracker,their address is {}.", instanceId, disconnectedPTs);
                taskPersistenceService.updateLostTasks(disconnectedPTs);
            }

            // 5.2 超时检查 -> 等待执行/执行中的任务（要不要采取 Worker不挂不行动准则，Worker挂了再重新派发任务）

        }

        @Override
        public void run() {
            try {
                innerRun();
            }catch (Exception e) {
                log.warn("[TaskTracker-{}] status checker execute failed, please fix the bug (@tjq)!", instanceInfo.getInstanceId(), e);
            }
        }
    }
}
