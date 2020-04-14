package com.github.kfcfans.oms.worker.core.tracker.task;

import akka.actor.ActorSelection;
import com.github.kfcfans.common.RemoteConstant;
import com.github.kfcfans.common.TimeExpressionType;
import com.github.kfcfans.common.model.InstanceDetail;
import com.github.kfcfans.common.request.ServerScheduleJobReq;
import com.github.kfcfans.common.utils.CommonUtils;
import com.github.kfcfans.oms.worker.OhMyWorker;
import com.github.kfcfans.oms.worker.common.constants.TaskConstant;
import com.github.kfcfans.oms.worker.common.constants.TaskStatus;
import com.github.kfcfans.oms.worker.common.utils.AkkaUtils;
import com.github.kfcfans.oms.worker.core.ha.ProcessorTrackerStatusHolder;
import com.github.kfcfans.oms.worker.persistence.TaskDO;
import com.github.kfcfans.oms.worker.persistence.TaskPersistenceService;
import com.github.kfcfans.oms.worker.pojo.model.InstanceInfo;
import com.github.kfcfans.oms.worker.pojo.request.ProcessorTrackerStatusReportReq;
import com.github.kfcfans.oms.worker.pojo.request.TaskTrackerStartTaskReq;
import com.github.kfcfans.oms.worker.pojo.request.TaskTrackerStopInstanceReq;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 负责管理 JobInstance 的运行，主要包括任务的派发（MR可能存在大量的任务）和状态的更新
 *
 * @author tjq
 * @since 2020/4/8
 */
@Slf4j
public abstract class TaskTracker {

    // TaskTracker创建时间
    protected long createTime;
    // 任务实例ID，使用频率过高，从 InstanceInfo 提取出来单独保存一份
    protected long instanceId;
    // 任务实例信息
    protected InstanceInfo instanceInfo;
    // ProcessTracker 状态管理
    protected ProcessorTrackerStatusHolder ptStatusHolder;
    // 数据库持久化服务
    protected TaskPersistenceService taskPersistenceService;
    // 定时任务线程池
    protected ScheduledExecutorService scheduledPool;
    // 是否结束
    protected AtomicBoolean finished;

    protected TaskTracker(ServerScheduleJobReq req) {

        // 初始化成员变量
        this.createTime = System.currentTimeMillis();
        this.instanceId = req.getInstanceId();
        this.instanceInfo = new InstanceInfo();
        BeanUtils.copyProperties(req, instanceInfo);
        // 特殊处理超时时间
        if (instanceInfo.getInstanceTimeoutMS() <= 0) {
            instanceInfo.setInstanceTimeoutMS(Long.MAX_VALUE);
        }
        this.ptStatusHolder = new ProcessorTrackerStatusHolder(req.getAllWorkerAddress());
        this.taskPersistenceService = TaskPersistenceService.INSTANCE;
        this.finished = new AtomicBoolean(false);

        // 子类自定义初始化操作
        initTaskTracker(req);

        log.info("[TaskTracker-{}] create TaskTracker from request({}) successfully.", req.getInstanceId(), req);
    }

    /**
     * 静态方法创建 TaskTracker
     * @param req 服务端调度任务请求
     * @return API/CRON -> CommonTaskTracker, FIX_RATE/FIX_DELAY -> FrequentTaskTracker
     */
    public static TaskTracker create(ServerScheduleJobReq req) {
        TimeExpressionType timeExpressionType = TimeExpressionType.valueOf(req.getTimeExpressionType());
        switch (timeExpressionType) {
            case FIX_RATE:
            case FIX_DELAY:return new FrequentTaskTracker(req);
            default:return new CommonTaskTracker(req);
        }
    }

    /* *************************** 对外方法区 *************************** */
    /**
     * 更新Task状态（任务状态机限定只允许状态变量递增，eg. 允许 FAILED -> SUCCEED，但不允许 SUCCEED -> FAILED）
     * @param taskId task的ID（task为任务实例的执行单位）
     * @param newStatus task的新状态
     * @param result task的执行结果，未执行完成时为空
     */
    public void updateTaskStatus(String taskId, int newStatus, @Nullable String result) {

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
            // 必存在，但不这么写，Java会警告...
            TaskStatus dbTaskStatus = dbTaskStatusOpt.orElse(TaskStatus.WAITING_DISPATCH);
            log.warn("[TaskTracker-{}] task(taskId={},dbStatus={},requestStatus={}) status conflict, TaskTracker won't update the status.",
                    instanceId, taskId, dbTaskStatus, nTaskStatus);
            return;
        }

        // 3. 失败重试处理
        if (nTaskStatus == TaskStatus.WORKER_PROCESS_FAILED) {

            // 数据库查询失败的话，就最多只重试一次
            int configTaskRetryNum = instanceInfo.getTaskRetryNum();
            int failedCnt = taskPersistenceService.getTaskFailedCnt(instanceId, taskId).orElse(configTaskRetryNum - 1);
            if (failedCnt < configTaskRetryNum && configTaskRetryNum > 1) {

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
     * 提交Task任务(MapReduce的Map，Broadcast的广播)，上层保证 batchSize，同时插入过多数据可能导致失败
     * @param newTaskList 新增的子任务列表
     */
    public boolean submitTask(List<TaskDO> newTaskList) {
        if (CollectionUtils.isEmpty(newTaskList)) {
            return true;
        }
        // 基础处理（多循环一次虽然有些浪费，但分布式执行中，这点耗时绝不是主要占比，忽略不计！）
        newTaskList.forEach(task -> {
            task.setInstanceId(instanceId);
            task.setStatus(TaskStatus.WAITING_DISPATCH.getValue());
            task.setFailedCnt(0);
            task.setLastModifiedTime(System.currentTimeMillis());
            task.setCreatedTime(System.currentTimeMillis());
        });

        log.debug("[TaskTracker-{}] receive new tasks: {}", instanceId, newTaskList);
        return taskPersistenceService.batchSave(newTaskList);
    }

    /**
     * 处理 ProcessorTracker 的心跳信息
     * @param heartbeatReq ProcessorTracker（任务的执行管理器）发来的心跳包，包含了其当前状态
     */
    public void receiveProcessorTrackerHeartbeat(ProcessorTrackerStatusReportReq heartbeatReq) {
        ptStatusHolder.updateStatus(heartbeatReq);
        log.debug("[TaskTracker-{}] receive heartbeat: {}", instanceId, heartbeatReq);
    }

    /**
     * 生成广播任务
     * @param preExecuteSuccess 预执行广播任务运行状态
     * @param subInstanceId 子实例ID
     * @param preTaskId 预执行广播任务的taskId
     * @param result 预执行广播任务的结果
     */
    public void broadcast(boolean preExecuteSuccess, long subInstanceId, String preTaskId, String result) {

        log.info("[TaskTracker-{}] finished broadcast's preProcess.", instanceId);

        // 1. 生成集群子任务
        if (preExecuteSuccess) {
            List<String> allWorkerAddress = ptStatusHolder.getAllProcessorTrackers();
            List<TaskDO> subTaskList = Lists.newLinkedList();
            for (int i = 0; i < allWorkerAddress.size(); i++) {
                TaskDO subTask = new TaskDO();
                subTask.setSubInstanceId(subInstanceId);
                subTask.setTaskName(TaskConstant.BROADCAST_TASK_NAME);
                subTask.setTaskId(preTaskId + "." + i);
                subTaskList.add(subTask);
            }
            submitTask(subTaskList);
        }else {
            log.debug("[TaskTracker-{}] BroadcastTask failed because of preProcess failed, preProcess result={}.", instanceId, result);
        }

        // 2. 更新根任务状态（广播任务的根任务为 preProcess 任务）
        int status = preExecuteSuccess ? TaskStatus.WORKER_PROCESS_SUCCESS.getValue() : TaskStatus.WORKER_PROCESS_FAILED.getValue();
        updateTaskStatus(preTaskId, status, result);
    }

    /**
     * 销毁自身，释放资源
     */
    public void destroy() {

        // 0. 开始关闭线程池，不能使用 shutdownNow()，因为 destroy 方法本身就在 scheduledPool 的线程中执行，强行关闭会打断 destroy 的执行。
        scheduledPool.shutdown();

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
            taskPersistenceService.deleteAllTasks(instanceId);
        }else {
            log.debug("[TaskTracker-{}] delete all tasks from database successfully.", instanceId);
        }

        // 3. 移除顶层引用，送去 GC
        TaskTrackerPool.remove(instanceId);

        log.info("[TaskTracker-{}] TaskTracker has left the world, bye~", instanceId);

        // 4. 强制关闭线程池
        if (!scheduledPool.isTerminated()) {
            CommonUtils.executeIgnoreException(() -> scheduledPool.shutdownNow());
        }

    }

    /* *************************** 对内方法区 *************************** */

    /**
     * 派发任务到 ProcessorTracker
     * @param task 需要被执行的任务
     * @param processorTrackerAddress ProcessorTracker的地址（IP:Port）
     */
    protected void dispatchTask(TaskDO task, String processorTrackerAddress) {

        TaskTrackerStartTaskReq startTaskReq = new TaskTrackerStartTaskReq(instanceInfo, task);

        String ptActorPath = AkkaUtils.getAkkaWorkerPath(processorTrackerAddress, RemoteConstant.PROCESSOR_TRACKER_ACTOR_NAME);
        ActorSelection ptActor = OhMyWorker.actorSystem.actorSelection(ptActorPath);
        ptActor.tell(startTaskReq, null);

        // 更新 ProcessorTrackerStatus 状态
        ptStatusHolder.getProcessorTrackerStatus(processorTrackerAddress).setDispatched(true);
        // 更新数据库（如果更新数据库失败，可能导致重复执行，先不处理）
        TaskDO updateEntity = new TaskDO();
        updateEntity.setStatus(TaskStatus.DISPATCH_SUCCESS_WORKER_UNCHECK.getValue());
        // 写入处理该任务的 ProcessorTracker
        updateEntity.setAddress(processorTrackerAddress);
        taskPersistenceService.updateTask(instanceId, task.getTaskId(), updateEntity);

        log.debug("[TaskTracker-{}] dispatch task(taskId={},taskName={}) successfully.", instanceId, task.getTaskId(), task.getTaskName());
    }

    /**
     * 获取任务实例产生的各个Task状态，用于分析任务实例执行情况
     * @param subInstanceId 子任务实例ID
     * @return InstanceStatisticsHolder
     */
    protected InstanceStatisticsHolder getInstanceStatisticsHolder(long subInstanceId) {

        Map<TaskStatus, Long> status2Num = taskPersistenceService.getTaskStatusStatistics(instanceId, subInstanceId);
        InstanceStatisticsHolder holder = new InstanceStatisticsHolder();

        holder.waitingDispatchNum = status2Num.getOrDefault(TaskStatus.WAITING_DISPATCH, 0L);
        holder.workerUnreceivedNum = status2Num.getOrDefault(TaskStatus.DISPATCH_SUCCESS_WORKER_UNCHECK, 0L);
        holder.receivedNum = status2Num.getOrDefault(TaskStatus.WORKER_RECEIVED, 0L);
        holder.runningNum = status2Num.getOrDefault(TaskStatus.WORKER_PROCESSING, 0L);
        holder.failedNum = status2Num.getOrDefault(TaskStatus.WORKER_PROCESS_FAILED, 0L);
        holder.succeedNum = status2Num.getOrDefault(TaskStatus.WORKER_PROCESS_SUCCESS, 0L);
        return holder;
    }



    /**
     * 定时扫描数据库中的task（出于内存占用量考虑，每次最多获取100个），并将需要执行的任务派发出去
     */
    protected class Dispatcher implements Runnable {

        // 数据库查询限制，每次最多查询几个任务
        private static final int DB_QUERY_LIMIT = 100;

        @Override
        public void run() {

            if (finished.get()) {
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
                    // 获取 ProcessorTracker 地址，如果 Task 中自带了 Address，则使用该 Address
                    String ptAddress = task.getAddress();
                    if (StringUtils.isEmpty(ptAddress) || RemoteConstant.EMPTY_ADDRESS.equals(ptAddress)) {
                        ptAddress = availablePtIps.get(index.getAndIncrement() % availablePtIps.size());
                    }
                    dispatchTask(task, ptAddress);
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
     * 存储任务实例产生的各个Task状态，用于分析任务实例执行情况
     */
    @Data
    protected static class InstanceStatisticsHolder {
        // 等待派发状态（仅存在 TaskTracker 数据库中）
        protected long waitingDispatchNum;
        // 已派发，但 ProcessorTracker 未确认，可能由于网络错误请求未送达，也有可能 ProcessorTracker 线程池满，拒绝执行
        protected long workerUnreceivedNum;
        // ProcessorTracker确认接收，存在与线程池队列中，排队执行
        protected long receivedNum;
        // ProcessorTracker正在执行
        protected long runningNum;
        protected long failedNum;
        protected long succeedNum;

        public long getTotalTaskNum() {
            return waitingDispatchNum + workerUnreceivedNum + receivedNum + runningNum + failedNum + succeedNum;
        }
    }

    /**
     * 初始化 TaskTracker
     * @param req 服务器调度任务实例运行请求
     */
    abstract protected void initTaskTracker(ServerScheduleJobReq req);

    /**
     * 查询任务实例的详细运行状态
     * @return 任务实例的详细运行状态
     */
    abstract public InstanceDetail fetchRunningStatus();
}
