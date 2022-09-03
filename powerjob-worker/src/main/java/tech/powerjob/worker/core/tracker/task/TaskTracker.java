package tech.powerjob.worker.core.tracker.task;

import akka.actor.ActorSelection;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import tech.powerjob.common.enums.ExecuteType;
import tech.powerjob.common.enums.InstanceStatus;
import tech.powerjob.common.RemoteConstant;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.model.InstanceDetail;
import tech.powerjob.common.request.ServerScheduleJobReq;
import tech.powerjob.common.request.TaskTrackerReportInstanceStatusReq;
import tech.powerjob.common.request.WorkerQueryExecutorClusterReq;
import tech.powerjob.common.response.AskResponse;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.common.utils.SegmentLock;
import tech.powerjob.worker.common.WorkerRuntime;
import tech.powerjob.worker.common.constants.TaskConstant;
import tech.powerjob.worker.common.constants.TaskStatus;
import tech.powerjob.worker.common.utils.AkkaUtils;
import tech.powerjob.worker.common.utils.WorkflowContextUtils;
import tech.powerjob.worker.core.ha.ProcessorTrackerStatusHolder;
import tech.powerjob.worker.persistence.TaskDO;
import tech.powerjob.worker.persistence.TaskPersistenceService;
import tech.powerjob.worker.pojo.model.InstanceInfo;
import tech.powerjob.worker.pojo.request.ProcessorTrackerStatusReportReq;
import tech.powerjob.worker.pojo.request.TaskTrackerStartTaskReq;
import tech.powerjob.worker.pojo.request.TaskTrackerStopInstanceReq;
import com.google.common.base.Stopwatch;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import java.util.Collections;
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

    /**
     * TaskTracker创建时间
     */
    protected final long createTime;
    /**
     * worker 运行时元数据
     */
    protected final WorkerRuntime workerRuntime;
    /**
     * 任务实例ID，使用频率过高，从 InstanceInfo 提取出来单独保存一份
     */
    protected final long instanceId;
    /**
     * 任务实例信息
     */
    protected final InstanceInfo instanceInfo;
    /**
     * ProcessTracker 状态管理
     */
    protected final ProcessorTrackerStatusHolder ptStatusHolder;
    /**
     * 数据库持久化服务
     */
    protected final TaskPersistenceService taskPersistenceService;
    /**
     * 定时任务线程池
     */
    protected ScheduledExecutorService scheduledPool;
    /**
     * 是否结束
     */
    protected final AtomicBoolean finished;
    /**
     * 追加的工作流上下文数据
     *
     * @since 2021/02/05
     */
    protected final Map<String, String> appendedWfContext;
    /**
     * 任务信息缓存
     */
    private final Cache<String, TaskBriefInfo> taskId2BriefInfo;


    /**
     * 分段锁
     */
    private final SegmentLock segmentLock;
    private static final int UPDATE_CONCURRENCY = 4;

    protected TaskTracker(ServerScheduleJobReq req, WorkerRuntime workerRuntime) {

        // 初始化成员变量
        this.createTime = System.currentTimeMillis();
        this.workerRuntime = workerRuntime;
        this.instanceId = req.getInstanceId();
        this.instanceInfo = new InstanceInfo();
        BeanUtils.copyProperties(req, instanceInfo);
        // 特殊处理超时时间
        if (instanceInfo.getInstanceTimeoutMS() <= 0) {
            // Integer最大值：2147483647，一天的毫秒数：86400000；够执行24天了...要是不满足需求就让开发者手动指定吧
            instanceInfo.setInstanceTimeoutMS(Integer.MAX_VALUE);
        }
        // 赋予时间表达式类型
        instanceInfo.setTimeExpressionType(TimeExpressionType.valueOf(req.getTimeExpressionType()).getV());
        // 保护性操作
        instanceInfo.setThreadConcurrency(Math.max(1, instanceInfo.getThreadConcurrency()));

        this.ptStatusHolder = new ProcessorTrackerStatusHolder(req.getAllWorkerAddress());
        this.taskPersistenceService = workerRuntime.getTaskPersistenceService();
        this.finished = new AtomicBoolean(false);
        // 只有工作流中的任务允许向工作流中追加上下文数据
        this.appendedWfContext = req.getWfInstanceId() == null ? Collections.emptyMap() : Maps.newConcurrentMap();
        // 构建缓存
        taskId2BriefInfo = CacheBuilder.newBuilder().maximumSize(1024).build();

        // 构建分段锁
        segmentLock = new SegmentLock(UPDATE_CONCURRENCY);

        // 子类自定义初始化操作
        initTaskTracker(req);

        log.info("[TaskTracker-{}] create TaskTracker successfully.", instanceId);
    }

    /**
     * 静态方法创建 TaskTracker
     *
     * @param req 服务端调度任务请求
     * @return API/CRON -> CommonTaskTracker, FIX_RATE/FIX_DELAY -> FrequentTaskTracker
     */
    public static TaskTracker create(ServerScheduleJobReq req, WorkerRuntime workerRuntime) {
        try {
            TimeExpressionType timeExpressionType = TimeExpressionType.valueOf(req.getTimeExpressionType());
            switch (timeExpressionType) {
                case FIXED_RATE:
                case FIXED_DELAY:
                    return new FrequentTaskTracker(req, workerRuntime);
                default:
                    return new CommonTaskTracker(req, workerRuntime);
            }
        } catch (Exception e) {
            log.warn("[TaskTracker-{}] create TaskTracker from request({}) failed.", req.getInstanceId(), req, e);

            // 直接发送失败请求
            TaskTrackerReportInstanceStatusReq response = new TaskTrackerReportInstanceStatusReq();
            BeanUtils.copyProperties(req, response);
            response.setInstanceStatus(InstanceStatus.FAILED.getV());
            response.setResult(String.format("init TaskTracker failed, reason: %s", e.toString()));
            response.setReportTime(System.currentTimeMillis());
            response.setStartTime(System.currentTimeMillis());
            response.setSourceAddress(workerRuntime.getWorkerAddress());

            String serverPath = AkkaUtils.getServerActorPath(workerRuntime.getServerDiscoveryService().getCurrentServerAddress());
            ActorSelection serverActor = workerRuntime.getActorSystem().actorSelection(serverPath);
            serverActor.tell(response, null);
        }
        return null;
    }

    /* *************************** 对外方法区 *************************** */

    /**
     * 更新追加的上下文数据
     *
     * @param newAppendedWfContext 追加的上下文数据
     * @since 2021/02/05
     */
    public void updateAppendedWfContext(Map<String, String> newAppendedWfContext) {

        // check
        if (instanceInfo.getWfInstanceId() == null || CollectionUtils.isEmpty(newAppendedWfContext)) {
            // 只有工作流中的任务才有存储的必要
            return;
        }
        // 检查追加的上下文大小是否超出限制
        if (WorkflowContextUtils.isExceededLengthLimit(appendedWfContext, workerRuntime.getWorkerConfig().getMaxAppendedWfContextLength())) {
            log.warn("[TaskTracker-{}]current length of appended workflow context data is greater than {}, this appended workflow context data will be ignore!", instanceInfo.getInstanceId(), workerRuntime.getWorkerConfig().getMaxAppendedWfContextLength());
            // ignore appended workflow context data
            return;
        }

        for (Map.Entry<String, String> entry : newAppendedWfContext.entrySet()) {
            String originValue = appendedWfContext.put(entry.getKey(), entry.getValue());
            log.info("[TaskTracker-{}] update appended workflow context data {} : {} -> {}", instanceInfo.getInstanceId(), entry.getKey(), originValue, entry.getValue());
        }

    }


    /**
     * 更新Task状态
     * V1.0.0 -> V1.0.1（e405e283ad7f97b0b4e5d369c7de884c0caf9192） 锁方案变更，从 synchronized (taskId.intern()) 修改为分段锁，能大大减少内存占用，损失的只有理论并发度而已
     *
     * @param subInstanceId 子任务实例ID
     * @param taskId        task的ID（task为任务实例的执行单位）
     * @param newStatus     task的新状态
     * @param reportTime    上报时间
     * @param result        task的执行结果，未执行完成时为空
     */
    @SuppressWarnings({"squid:S3776", "squid:S2142"})
    public void updateTaskStatus(Long subInstanceId, String taskId, int newStatus, long reportTime, @Nullable String result) {

        if (finished.get()) {
            return;
        }
        TaskStatus nTaskStatus = TaskStatus.of(newStatus);

        int lockId = taskId.hashCode();
        try {

            // 阻塞获取锁
            segmentLock.lockInterruptible(lockId);
            TaskBriefInfo taskBriefInfo = taskId2BriefInfo.getIfPresent(taskId);

            // 缓存中不存在，从数据库查
            if (taskBriefInfo == null) {
                Optional<TaskDO> taskOpt = taskPersistenceService.getTask(instanceId, taskId);
                if (taskOpt.isPresent()) {
                    TaskDO taskDO = taskOpt.get();
                    taskBriefInfo = new TaskBriefInfo(taskId, TaskStatus.of(taskDO.getStatus()), taskDO.getLastReportTime());
                } else {
                    // 理论上不存在这种情况，除非数据库异常
                    log.error("[TaskTracker-{}-{}] can't find task by taskId={}.", instanceId, subInstanceId, taskId);
                    taskBriefInfo = new TaskBriefInfo(taskId, TaskStatus.WAITING_DISPATCH, -1L);
                }
                // 写入缓存
                taskId2BriefInfo.put(taskId, taskBriefInfo);
            }

            // 过滤过期的请求（潜在的集群时间一致性需求，重试跨 Worker 时，时间不一致可能导致问题）
            if (taskBriefInfo.getLastReportTime() > reportTime) {
                log.warn("[TaskTracker-{}-{}] receive expired(last {} > current {}) task status report(taskId={},newStatus={}), TaskTracker will drop this report.",
                        instanceId, subInstanceId, taskBriefInfo.getLastReportTime(), reportTime, taskId, newStatus);
                return;
            }
            // 检查状态转移是否合法，fix issue 404
            if (nTaskStatus.getValue() < taskBriefInfo.getStatus().getValue()) {
                log.warn("[TaskTracker-{}-{}] receive invalid task status report(taskId={},currentStatus={},newStatus={}), TaskTracker will drop this report.",
                        instanceId, subInstanceId, taskId, taskBriefInfo.getStatus().getValue(), newStatus);
                return;
            }

            // 此时本次请求已经有效，先更新相关信息
            taskBriefInfo.setLastReportTime(reportTime);
            taskBriefInfo.setStatus(nTaskStatus);

            // 处理失败的情况
            int configTaskRetryNum = instanceInfo.getTaskRetryNum();
            if (nTaskStatus == TaskStatus.WORKER_PROCESS_FAILED && configTaskRetryNum >= 1) {

                // 失败不是主要的情况，多查一次数据库也问题不大（况且前面有缓存顶着，大部分情况之前不会去查DB）
                Optional<TaskDO> taskOpt = taskPersistenceService.getTask(instanceId, taskId);
                // 查询DB再失败的话，就不重试了...
                if (taskOpt.isPresent()) {
                    int failedCnt = taskOpt.get().getFailedCnt();
                    if (failedCnt < configTaskRetryNum) {

                        TaskDO updateEntity = new TaskDO();
                        updateEntity.setFailedCnt(failedCnt + 1);

                        /*
                        地址规则：
                        1. 当前存储的地址为任务派发的目的地（ProcessorTracker地址）
                        2. 根任务、最终任务必须由TaskTracker所在机器执行（如果是根任务和最终任务，不应当修改地址）
                        3. 广播任务每台机器都需要执行，因此不应该重新分配worker（广播任务不应当修改地址）
                         */
                        String taskName = taskOpt.get().getTaskName();
                        ExecuteType executeType = ExecuteType.valueOf(instanceInfo.getExecuteType());
                        if (!taskName.equals(TaskConstant.ROOT_TASK_NAME) && !taskName.equals(TaskConstant.LAST_TASK_NAME) && executeType != ExecuteType.BROADCAST) {
                            updateEntity.setAddress(RemoteConstant.EMPTY_ADDRESS);
                        }

                        updateEntity.setStatus(TaskStatus.WAITING_DISPATCH.getValue());
                        updateEntity.setLastReportTime(reportTime);

                        boolean retryTask = taskPersistenceService.updateTask(instanceId, taskId, updateEntity);
                        if (retryTask) {
                            log.info("[TaskTracker-{}-{}] task(taskId={}) process failed, TaskTracker will have a retry.", instanceId, subInstanceId, taskId);
                            return;
                        }
                    }
                }
            }

            // 更新状态（失败重试写入DB失败的，也就不重试了...谁让你那么倒霉呢...）
            result = result == null ? "" : result;
            boolean updateResult = taskPersistenceService.updateTaskStatus(instanceId, taskId, newStatus, reportTime, result);

            if (!updateResult) {
                log.warn("[TaskTracker-{}-{}] update task status failed, this task(taskId={}) may be processed repeatedly!", instanceId, subInstanceId, taskId);
            }

        } catch (InterruptedException ignore) {
            // ignore
        } catch (Exception e) {
            log.warn("[TaskTracker-{}-{}] update task status failed.", instanceId, subInstanceId, e);
        } finally {
            segmentLock.unlock(lockId);
        }
    }

    /**
     * 提交Task任务(MapReduce的Map，Broadcast的广播)，上层保证 batchSize，同时插入过多数据可能导致失败
     *
     * @param newTaskList 新增的子任务列表
     */
    public boolean submitTask(List<TaskDO> newTaskList) {
        if (finished.get()) {
            return true;
        }
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
            task.setLastReportTime(-1L);
        });

        log.debug("[TaskTracker-{}] receive new tasks: {}", instanceId, newTaskList);
        return taskPersistenceService.batchSave(newTaskList);
    }

    /**
     * 处理 ProcessorTracker 的心跳信息
     *
     * @param heartbeatReq ProcessorTracker（任务的执行管理器）发来的心跳包，包含了其当前状态
     */
    public void receiveProcessorTrackerHeartbeat(ProcessorTrackerStatusReportReq heartbeatReq) {
        log.debug("[TaskTracker-{}] receive heartbeat: {}", instanceId, heartbeatReq);
        ptStatusHolder.updateStatus(heartbeatReq);

        // 上报空闲，检查是否已经接收到全部该 ProcessorTracker 负责的任务
        if (heartbeatReq.getType() == ProcessorTrackerStatusReportReq.IDLE) {
            String idlePtAddress = heartbeatReq.getAddress();
            // 该 ProcessorTracker 已销毁，重置为初始状态
            ptStatusHolder.getProcessorTrackerStatus(idlePtAddress).setDispatched(false);
            List<TaskDO> unfinishedTask = taskPersistenceService.getAllUnFinishedTaskByAddress(instanceId, idlePtAddress);
            if (!CollectionUtils.isEmpty(unfinishedTask)) {
                log.warn("[TaskTracker-{}] ProcessorTracker({}) is idle now but have unfinished tasks: {}", instanceId, idlePtAddress, unfinishedTask);
                unfinishedTask.forEach(task -> updateTaskStatus(task.getSubInstanceId(), task.getTaskId(), TaskStatus.WORKER_PROCESS_FAILED.getValue(), System.currentTimeMillis(), "SYSTEM: unreceived process result"));
            }
        }
    }

    /**
     * 生成广播任务
     *
     * @param preExecuteSuccess 预执行广播任务运行状态
     * @param subInstanceId     子实例ID
     * @param preTaskId         预执行广播任务的taskId
     * @param result            预执行广播任务的结果
     */
    public void broadcast(boolean preExecuteSuccess, long subInstanceId, String preTaskId, String result) {

        if (finished.get()) {
            return;
        }

        log.info("[TaskTracker-{}-{}] finished broadcast's preProcess, preExecuteSuccess:{},preTaskId:{},result:{}", instanceId, subInstanceId, preExecuteSuccess, preTaskId, result);

        // 生成集群子任务
        if (preExecuteSuccess) {
            List<String> allWorkerAddress = ptStatusHolder.getAllProcessorTrackers();
            List<TaskDO> subTaskList = Lists.newLinkedList();
            for (int i = 0; i < allWorkerAddress.size(); i++) {
                TaskDO subTask = new TaskDO();
                subTask.setSubInstanceId(subInstanceId);
                subTask.setTaskName(TaskConstant.BROADCAST_TASK_NAME);
                subTask.setTaskId(preTaskId + "." + i);
                // 广播任务直接写入派发地址
                subTask.setAddress(allWorkerAddress.get(i));
                subTaskList.add(subTask);
            }
            submitTask(subTaskList);
        } else {
            log.warn("[TaskTracker-{}-{}] BroadcastTask failed because of preProcess failed, preProcess result={}.", instanceId, subInstanceId, result);
        }
    }

    /**
     * 销毁自身，释放资源
     */
    public void destroy() {

        finished.set(true);

        Stopwatch sw = Stopwatch.createStarted();
        // 0. 开始关闭线程池，不能使用 shutdownNow()，因为 destroy 方法本身就在 scheduledPool 的线程中执行，强行关闭会打断 destroy 的执行。
        scheduledPool.shutdown();

        // 1. 通知 ProcessorTracker 释放资源
        TaskTrackerStopInstanceReq stopRequest = new TaskTrackerStopInstanceReq();
        stopRequest.setInstanceId(instanceId);
        ptStatusHolder.getAllProcessorTrackers().forEach(ptIP -> {
            String ptPath = AkkaUtils.getAkkaWorkerPath(ptIP, RemoteConstant.PROCESSOR_TRACKER_ACTOR_NAME);
            ActorSelection ptActor = workerRuntime.getActorSystem().actorSelection(ptPath);
            // 不可靠通知，ProcessorTracker 也可以靠自己的定时任务/问询等方式关闭
            ptActor.tell(stopRequest, null);
        });

        // 2. 删除所有数据库数据
        boolean dbSuccess = taskPersistenceService.deleteAllTasks(instanceId);
        if (!dbSuccess) {
            log.error("[TaskTracker-{}] delete tasks from database failed.", instanceId);
        } else {
            log.debug("[TaskTracker-{}] delete all tasks from database successfully.", instanceId);
        }

        // 3. 移除顶层引用，送去 GC
        TaskTrackerPool.remove(instanceId);

        log.info("[TaskTracker-{}] TaskTracker has left the world(using {}), bye~", instanceId, sw.stop());

        // 4. 强制关闭线程池
        if (!scheduledPool.isTerminated()) {
            CommonUtils.executeIgnoreException(() -> scheduledPool.shutdownNow());
        }

    }

    /* *************************** 对内方法区 *************************** */

    /**
     * 派发任务到 ProcessorTracker
     *
     * @param task                    需要被执行的任务
     * @param processorTrackerAddress ProcessorTracker的地址（IP:Port）
     */
    protected void dispatchTask(TaskDO task, String processorTrackerAddress) {

        // 1. 持久化，更新数据库（如果更新数据库失败，可能导致重复执行，先不处理）
        TaskDO updateEntity = new TaskDO();
        updateEntity.setStatus(TaskStatus.DISPATCH_SUCCESS_WORKER_UNCHECK.getValue());
        // 写入处理该任务的 ProcessorTracker
        updateEntity.setAddress(processorTrackerAddress);
        boolean success = taskPersistenceService.updateTask(instanceId, task.getTaskId(), updateEntity);
        if (!success) {
            log.warn("[TaskTracker-{}] dispatch task(taskId={},taskName={}) failed due to update task status failed.", instanceId, task.getTaskId(), task.getTaskName());
            return;
        }

        // 2. 更新 ProcessorTrackerStatus 状态
        ptStatusHolder.getProcessorTrackerStatus(processorTrackerAddress).setDispatched(true);
        // 3. 初始化缓存
        taskId2BriefInfo.put(task.getTaskId(), new TaskBriefInfo(task.getTaskId(), TaskStatus.DISPATCH_SUCCESS_WORKER_UNCHECK, -1L));

        // 4. 任务派发
        TaskTrackerStartTaskReq startTaskReq = new TaskTrackerStartTaskReq(instanceInfo, task, workerRuntime.getWorkerAddress());
        String ptActorPath = AkkaUtils.getAkkaWorkerPath(processorTrackerAddress, RemoteConstant.PROCESSOR_TRACKER_ACTOR_NAME);
        ActorSelection ptActor = workerRuntime.getActorSystem().actorSelection(ptActorPath);
        ptActor.tell(startTaskReq, null);

        log.debug("[TaskTracker-{}] dispatch task(taskId={},taskName={}) successfully.", instanceId, task.getTaskId(), task.getTaskName());
    }

    /**
     * 获取任务实例产生的各个Task状态，用于分析任务实例执行情况
     *
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

            // 1. 获取可以派发任务的 ProcessorTracker
            List<String> availablePtIps = ptStatusHolder.getAvailableProcessorTrackers();

            // 2. 没有可用 ProcessorTracker，本次不派发
            if (availablePtIps.isEmpty()) {
                log.debug("[TaskTracker-{}] no available ProcessorTracker now.", instanceId);
                return;
            }

            // 3. 避免大查询，分批派发任务
            long currentDispatchNum = 0;
            long maxDispatchNum = availablePtIps.size() * instanceInfo.getThreadConcurrency() * 2L;
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
                    break;
                }
            }

            log.debug("[TaskTracker-{}] dispatched {} tasks,using time {}.", instanceId, currentDispatchNum, stopwatch.stop());
        }
    }

    /**
     * 执行器动态上线（for 秒级任务和 MR 任务）
     * 原则：server 查询得到的 执行器状态不会干预 worker 自己维护的状态，即只做新增，不做任何修改
     */
    protected class WorkerDetector implements Runnable {
        @Override
        public void run() {
            String serverPath = AkkaUtils.getServerActorPath(workerRuntime.getServerDiscoveryService().getCurrentServerAddress());
            if (StringUtils.isEmpty(serverPath)) {
                log.warn("[TaskTracker-{}] no server available, won't start worker detective!", instanceId);
                return;
            }
            WorkerQueryExecutorClusterReq req = new WorkerQueryExecutorClusterReq(workerRuntime.getAppId(), instanceInfo.getJobId());
            AskResponse response = AkkaUtils.easyAsk(workerRuntime.getActorSystem().actorSelection(serverPath), req);
            if (!response.isSuccess()) {
                log.warn("[TaskTracker-{}] detective failed due to ask failed, message is {}", instanceId, response.getMessage());
                return;
            }
            try {
                List<String> workerList = JsonUtils.parseObject(response.getData(), new TypeReference<List<String>>() {});
                workerList.forEach(address -> {
                    if (ptStatusHolder.register(address)) {
                        log.info("[TaskTracker-{}] detective new worker: {}", instanceId, address);
                    }
                });
            } catch (Exception e) {
                log.warn("[TaskTracker-{}] detective failed!", instanceId, e);
            }
        }
    }

    @Data
    @AllArgsConstructor
    protected static class TaskBriefInfo {

        private String id;

        private TaskStatus status;

        private Long lastReportTime;
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
     *
     * @param req 服务器调度任务实例运行请求
     */
    protected abstract void initTaskTracker(ServerScheduleJobReq req);

    /**
     * 查询任务实例的详细运行状态
     *
     * @return 任务实例的详细运行状态
     */
    public abstract InstanceDetail fetchRunningStatus();
}
