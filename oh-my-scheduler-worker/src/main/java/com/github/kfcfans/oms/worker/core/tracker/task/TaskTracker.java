package com.github.kfcfans.oms.worker.core.tracker.task;

import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import com.github.kfcfans.common.ExecuteType;
import com.github.kfcfans.common.JobInstanceStatus;
import com.github.kfcfans.common.request.TaskTrackerReportInstanceStatusReq;
import com.github.kfcfans.common.response.AskResponse;
import com.github.kfcfans.oms.worker.OhMyWorker;
import com.github.kfcfans.common.AkkaConstant;
import com.github.kfcfans.oms.worker.common.constants.CommonSJ;
import com.github.kfcfans.oms.worker.common.constants.TaskConstant;
import com.github.kfcfans.oms.worker.common.constants.TaskStatus;
import com.github.kfcfans.oms.worker.common.utils.AkkaUtils;
import com.github.kfcfans.oms.worker.common.utils.NetUtils;
import com.github.kfcfans.oms.worker.persistence.TaskDO;
import com.github.kfcfans.oms.worker.persistence.TaskPersistenceService;
import com.github.kfcfans.oms.worker.pojo.model.JobInstanceInfo;
import com.github.kfcfans.oms.worker.pojo.request.TaskTrackerStartTaskReq;
import com.github.kfcfans.oms.worker.pojo.request.TaskTrackerStopInstanceReq;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import java.time.Duration;
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
@ToString
public class TaskTracker {

    protected long startTime;
    protected long jobTimeLimitMS;

    // 任务实例信息
    protected JobInstanceInfo jobInstanceInfo;

    @Getter
    protected List<String> allWorkerAddress;

    protected TaskPersistenceService taskPersistenceService;
    protected ScheduledExecutorService scheduledPool;

    protected AtomicBoolean finished = new AtomicBoolean(false);

    public TaskTracker(JobInstanceInfo jobInstanceInfo) {

        log.info("[TaskTracker] start to create TaskTracker for instance({}).", jobInstanceInfo);

        this.startTime = System.currentTimeMillis();
        this.jobTimeLimitMS = jobInstanceInfo.getTimeLimit();

        this.jobInstanceInfo = jobInstanceInfo;
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
     * 任务状态机只允许数字递增
     */
    public void updateTaskStatus(String instanceId, String taskId, int status, @Nullable String result, boolean force) {

        // 1. 读取当前Task状态，防止过期消息重置任务状态
        if (!force) {
            TaskDO originTask = taskPersistenceService.selectTaskByKey(instanceId, taskId);
            if (originTask.getStatus() > status) {
                log.warn("[TaskTracker] task(instanceId={},taskId={},dbStatus={},requestStatus={}) status conflict, this request will be drop.",
                        instanceId, taskId, originTask.getStatus(), status);
                return;
            }
        }

        TaskStatus taskStatus = TaskStatus.of(status);

        // 2. 更新数据库状态
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
        rootTask.setLastModifiedTime(System.currentTimeMillis());

        if (!taskPersistenceService.save(rootTask)) {
            throw new RuntimeException("create root task failed.");
        }
        log.info("[TaskTracker] create root task successfully for instance(instanceId={}).", jobInstanceInfo.getInstanceId());
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

            taskPersistenceService.getTaskByStatus(jobInstanceInfo.getInstanceId(), TaskStatus.WAITING_DISPATCH, 100).forEach(task -> {
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
                    targetActor.tell(req, null);

                    // 更新数据库（如果更新数据库失败，可能导致重复执行，先不处理）
                    taskPersistenceService.updateTaskStatus(task.getInstanceId(), task.getTaskId(), TaskStatus.DISPATCH_SUCCESS_WORKER_UNCHECK, null);

                    log.debug("[TaskTracker] dispatch task({instanceId={},taskId={},taskName={}} successfully.)", task.getInstanceId(), task.getTaskId(), task.getTaskName());
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

        private static final long TIME_OUT_MS = 5000;


        private void innerRun() {

            final String instanceId = jobInstanceInfo.getInstanceId();

            // 1. 查询统计信息
            Map<TaskStatus, Long> status2Num = taskPersistenceService.getTaskStatusStatistics(instanceId);

            long waitingDispatchNum = status2Num.getOrDefault(TaskStatus.WAITING_DISPATCH, 0L);
            long workerUnreceivedNum = status2Num.getOrDefault(TaskStatus.DISPATCH_SUCCESS_WORKER_UNCHECK, 0L);
            long receivedNum = status2Num.getOrDefault(TaskStatus.WORKER_RECEIVED, 0L);
            long runningNum = status2Num.getOrDefault(TaskStatus.WORKER_PROCESSING, 0L);
            long succeedNum = status2Num.getOrDefault(TaskStatus.WORKER_PROCESS_SUCCESS, 0L);
            long failedNum = status2Num.getOrDefault(TaskStatus.WORKER_PROCESS_FAILED, 0L);

            long finishedNum = succeedNum + failedNum;
            long unfinishedNum = waitingDispatchNum + workerUnreceivedNum + receivedNum + runningNum;

            log.debug("[TaskTracker] status check result: {}", status2Num);

            TaskTrackerReportInstanceStatusReq req = new TaskTrackerReportInstanceStatusReq();
            req.setJobId(jobInstanceInfo.getJobId());
            req.setInstanceId(instanceId);
            req.setTotalTaskNum(finishedNum + unfinishedNum);
            req.setSucceedTaskNum(succeedNum);
            req.setFailedTaskNum(failedNum);

            // 2. 如果未完成任务数为0，判断是否真正结束，并获取真正结束任务的执行结果
            TaskDO resultTask = null;
            if (unfinishedNum == 0) {

                boolean finishedBoolean = true;
                ExecuteType executeType = ExecuteType.valueOf(jobInstanceInfo.getExecuteType());

                if (executeType == ExecuteType.STANDALONE) {

                    List<TaskDO> allTask = taskPersistenceService.getAllTask(instanceId);
                    if (CollectionUtils.isEmpty(allTask) || allTask.size() > 1) {
                        log.warn("[TaskTracker] there must have some bug in TaskTracker.");
                    }else {
                        resultTask = allTask.get(0);
                    }

                } else {
                    resultTask = taskPersistenceService.getLastTask(instanceId);

                    // 不存在，代表前置任务刚刚执行完毕，需要创建 lastTask
                    if (resultTask == null) {

                        finishedBoolean = false;

                        TaskDO newLastTask = new TaskDO();
                        newLastTask.setTaskName(TaskConstant.LAST_TASK_NAME);
                        newLastTask.setTaskId(TaskConstant.LAST_TASK_ID);
                        newLastTask.setAddress(NetUtils.getLocalHost());
                        addTask(Lists.newArrayList(newLastTask));
                    }else {
                        TaskStatus lastTaskStatus = TaskStatus.of(resultTask.getStatus());
                        finishedBoolean = lastTaskStatus == TaskStatus.WORKER_PROCESS_SUCCESS || lastTaskStatus == TaskStatus.WORKER_PROCESS_FAILED;
                    }
                }
                finished.set(finishedBoolean);
            }

            String serverPath = AkkaUtils.getAkkaServerNodePath(AkkaConstant.SERVER_ACTOR_NAME);
            ActorSelection serverActor = OhMyWorker.actorSystem.actorSelection(serverPath);

            // 3. 执行成功，报告服务器
            if (finished.get() && resultTask != null) {

                boolean success = resultTask.getStatus() == TaskStatus.WORKER_PROCESS_SUCCESS.getValue();
                req.setResult(resultTask.getResult());
                req.setInstanceStatus(success ? JobInstanceStatus.SUCCEED.getValue() : JobInstanceStatus.FAILED.getValue());

                CompletionStage<Object> askCS = Patterns.ask(serverActor, req, Duration.ofMillis(TIME_OUT_MS));

                boolean serverAccepted = false;
                try {
                    AskResponse askResponse = (AskResponse) askCS.toCompletableFuture().get(TIME_OUT_MS, TimeUnit.MILLISECONDS);
                    serverAccepted = askResponse.isSuccess();
                }catch (Exception e) {
                    log.warn("[TaskTracker] report finished instance(id={}&result={}) failed.", instanceId, resultTask.getResult());
                }

                // 服务器未接受上报，则等待下次重新上报
                if (!serverAccepted) {
                    return;
                }

                // 服务器已经更新状态，任务已经执行完毕，开始释放所有资源
                log.info("[TaskTracker] instance(jobId={}&instanceId={}) process finished,result = {}, start to release resource...",
                        jobInstanceInfo.getJobId(), instanceId, resultTask.getResult());
                TaskTrackerStopInstanceReq stopRequest = new TaskTrackerStopInstanceReq();
                stopRequest.setInstanceId(instanceId);
                allWorkerAddress.forEach(ptIP -> {
                    String ptPath = AkkaUtils.getAkkaRemotePath(ptIP, AkkaConstant.PROCESSOR_TRACKER_ACTOR_NAME);
                    ActorSelection ptActor = OhMyWorker.actorSystem.actorSelection(ptPath);
                    // 不可靠通知，ProcessorTracker 也可以靠自己的定时任务/问询等方式关闭
                    ptActor.tell(stopRequest, null);
                });

                // 销毁TaskTracker
                TaskTrackerPool.remove(instanceId);
                destroy();

                return;
            }

            // 4. 未完成，上报状态
            req.setInstanceStatus(JobInstanceStatus.RUNNING.getValue());
            serverActor.tell(req, null);

            // 5.1 超时检查 -> 派发未接受的任务
            long currentMS = System.currentTimeMillis();
            if (workerUnreceivedNum != 0) {
                taskPersistenceService.getTaskByStatus(instanceId, TaskStatus.DISPATCH_SUCCESS_WORKER_UNCHECK, 100).forEach(uncheckTask -> {

                    long elapsedTime = currentMS - uncheckTask.getLastModifiedTime();
                    if (elapsedTime > TIME_OUT_MS) {
                        updateTaskStatus(instanceId, uncheckTask.getTaskId(), TaskStatus.WAITING_DISPATCH.getValue(), null, true);
                        log.warn("[TaskTracker] task(instanceId={},taskId={}) try to dispatch again due to unreceived the response from processor tracker.",
                                instanceId, uncheckTask.getTaskId());
                    }

                });
            }

            // 5.2 超时检查 -> 等待执行/执行中的任务（要不要采取 Worker不挂不行动准则，Worker挂了再重新派发任务）

        }

        @Override
        public void run() {
            try {
                innerRun();
            }catch (Exception e) {
                log.warn("[TaskTracker] status checker execute failed, please fix the bug (@tjq)!", e);
            }
        }
    }
}
