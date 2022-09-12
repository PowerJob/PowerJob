package tech.powerjob.worker.core.tracker.task;

import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import tech.powerjob.common.PowerJobDKey;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.RemoteConstant;
import tech.powerjob.common.SystemInstanceResult;
import tech.powerjob.common.enums.ExecuteType;
import tech.powerjob.common.enums.InstanceStatus;
import tech.powerjob.common.model.InstanceDetail;
import tech.powerjob.common.request.ServerScheduleJobReq;
import tech.powerjob.common.request.TaskTrackerReportInstanceStatusReq;
import tech.powerjob.common.response.AskResponse;
import tech.powerjob.worker.common.WorkerRuntime;
import tech.powerjob.worker.common.constants.TaskConstant;
import tech.powerjob.worker.common.constants.TaskStatus;
import tech.powerjob.worker.common.utils.AkkaUtils;
import tech.powerjob.worker.persistence.TaskDO;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * 负责管理 JobInstance 的运行，主要包括任务的派发（MR可能存在大量的任务）和状态的更新
 *
 * @author tjq
 * @since 2020/3/17
 */
@Slf4j
@ToString
public class CommonTaskTracker extends TaskTracker {

    /**
     * 连续上报多次失败后放弃上报，视为结果不可达，TaskTracker down
     */
    private int reportFailedCnt = 0;
    /**
     * 根任务 ID
     */
    public static final String ROOT_TASK_ID = "0";
    /**
     * 最后一个任务 ID
     * 除 {@link #ROOT_TASK_ID} 外任何数都可以
     */
    public static final String LAST_TASK_ID = "9999";

    private static final int MAX_REPORT_FAILED_THRESHOLD = 5;

    protected CommonTaskTracker(ServerScheduleJobReq req, WorkerRuntime workerRuntime) {
        super(req, workerRuntime);
    }

    @Override
    protected void initTaskTracker(ServerScheduleJobReq req) {

        // CommonTaskTrackerTimingPool 缩写
        String poolName = String.format("ctttp-%d", req.getInstanceId()) + "-%d";
        ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat(poolName).build();
        this.scheduledPool = Executors.newScheduledThreadPool(2, factory);

        // 持久化根任务
        persistenceRootTask();

        // 开启定时状态检查
        int delay = Integer.parseInt(System.getProperty(PowerJobDKey.WORKER_STATUS_CHECK_PERIOD, "13"));
        scheduledPool.scheduleWithFixedDelay(new StatusCheckRunnable(), 3, delay, TimeUnit.SECONDS);

        // 如果是 MR 任务，则需要启动执行器动态检测装置
        ExecuteType executeType = ExecuteType.valueOf(req.getExecuteType());
        if (executeType == ExecuteType.MAP || executeType == ExecuteType.MAP_REDUCE) {
            scheduledPool.scheduleAtFixedRate(new WorkerDetector(), 1, 1, TimeUnit.MINUTES);
        }

        // 最后启动任务派发器，否则会出现 TaskTracker 还未创建完毕 ProcessorTracker 已开始汇报状态的情况
        scheduledPool.scheduleWithFixedDelay(new Dispatcher(), 10, 5000, TimeUnit.MILLISECONDS);
    }

    @Override
    public InstanceDetail fetchRunningStatus() {

        InstanceDetail detail = new InstanceDetail();
        // 填充基础信息
        detail.setActualTriggerTime(createTime);
        detail.setStatus(InstanceStatus.RUNNING.getV());
        detail.setTaskTrackerAddress(workerRuntime.getWorkerAddress());

        // 填充详细信息
        InstanceStatisticsHolder holder = getInstanceStatisticsHolder(instanceId);
        InstanceDetail.TaskDetail taskDetail = new InstanceDetail.TaskDetail();
        taskDetail.setSucceedTaskNum(holder.succeedNum);
        taskDetail.setFailedTaskNum(holder.failedNum);
        taskDetail.setTotalTaskNum(holder.getTotalTaskNum());
        detail.setTaskDetail(taskDetail);

        return detail;
    }


    /**
     * 任务是否超时
     */
    public boolean isTimeout() {
        return System.currentTimeMillis() - createTime > instanceInfo.getInstanceTimeoutMS();
    }

    /**
     * 持久化根任务，只有完成持久化才能视为任务开始running（先持久化，再报告server）
     */
    private void persistenceRootTask() {

        TaskDO rootTask = new TaskDO();
        rootTask.setStatus(TaskStatus.WAITING_DISPATCH.getValue());
        rootTask.setInstanceId(instanceInfo.getInstanceId());
        rootTask.setTaskId(ROOT_TASK_ID);
        rootTask.setFailedCnt(0);
        rootTask.setAddress(workerRuntime.getWorkerAddress());
        rootTask.setTaskName(TaskConstant.ROOT_TASK_NAME);
        rootTask.setCreatedTime(System.currentTimeMillis());
        rootTask.setLastModifiedTime(System.currentTimeMillis());
        rootTask.setLastReportTime(-1L);
        rootTask.setSubInstanceId(instanceId);

        if (taskPersistenceService.save(rootTask)) {
            log.info("[TaskTracker-{}] create root task successfully.", instanceId);
        }else {
            log.error("[TaskTracker-{}] create root task failed.", instanceId);
            throw new PowerJobException("create root task failed for instance: " + instanceId);
        }
    }


    /**
     * 定时检查当前任务的执行状态
     */
    private class StatusCheckRunnable implements Runnable {

        private static final long DISPATCH_TIME_OUT_MS = 15000;

        @SuppressWarnings("squid:S3776")
        private void innerRun() {

            InstanceStatisticsHolder holder = getInstanceStatisticsHolder(instanceId);

            long finishedNum = holder.succeedNum + holder.failedNum;
            long unfinishedNum = holder.waitingDispatchNum + holder.workerUnreceivedNum + holder.receivedNum + holder.runningNum;

            log.debug("[TaskTracker-{}] status check result: {}", instanceId, holder);

            TaskTrackerReportInstanceStatusReq req = new TaskTrackerReportInstanceStatusReq();
            req.setAppId(workerRuntime.getAppId());
            req.setJobId(instanceInfo.getJobId());
            req.setInstanceId(instanceId);
            req.setWfInstanceId(instanceInfo.getWfInstanceId());
            req.setTotalTaskNum(finishedNum + unfinishedNum);
            req.setSucceedTaskNum(holder.succeedNum);
            req.setFailedTaskNum(holder.failedNum);
            req.setReportTime(System.currentTimeMillis());
            req.setStartTime(createTime);
            req.setSourceAddress(workerRuntime.getWorkerAddress());

            boolean success = false;
            String result = null;

            // 2. 如果未完成任务数为0，判断是否真正结束，并获取真正结束任务的执行结果
            if (unfinishedNum == 0) {

                // 数据库中一个任务都没有，说明根任务创建失败，该任务实例失败
                if (finishedNum == 0) {
                    finished.set(true);
                    result = SystemInstanceResult.TASK_INIT_FAILED;
                }else {
                    ExecuteType executeType = ExecuteType.valueOf(instanceInfo.getExecuteType());

                    switch (executeType) {

                        // STANDALONE 只有一个任务，完成即结束
                        case STANDALONE:
                            finished.set(true);
                            List<TaskDO> allTask = taskPersistenceService.getAllTask(instanceId, instanceId);
                            if (CollectionUtils.isEmpty(allTask) || allTask.size() > 1) {
                                success = false;
                                result = SystemInstanceResult.UNKNOWN_BUG;
                                log.warn("[TaskTracker-{}] there must have some bug in TaskTracker.", instanceId);
                            }else {
                                result = allTask.get(0).getResult();
                                success = allTask.get(0).getStatus() == TaskStatus.WORKER_PROCESS_SUCCESS.getValue();
                            }
                            break;
                        // MAP 不关心结果，最简单
                        case MAP:
                            finished.set(true);
                            success = holder.failedNum == 0;
                            result = String.format("total:%d,succeed:%d,failed:%d", holder.getTotalTaskNum(), holder.succeedNum, holder.failedNum);
                            break;
                        // MapReduce 和 Broadcast 任务实例是否完成根据**LastTask**的执行情况判断
                        default:

                            Optional<TaskDO> lastTaskOptional = taskPersistenceService.getLastTask(instanceId, instanceId);
                            if (lastTaskOptional.isPresent()) {

                                // 存在则根据 reduce 任务来判断状态
                                TaskDO resultTask = lastTaskOptional.get();
                                TaskStatus lastTaskStatus = TaskStatus.of(resultTask.getStatus());

                                if (lastTaskStatus == TaskStatus.WORKER_PROCESS_SUCCESS || lastTaskStatus == TaskStatus.WORKER_PROCESS_FAILED) {
                                    finished.set(true);
                                    success = lastTaskStatus == TaskStatus.WORKER_PROCESS_SUCCESS;
                                    result = resultTask.getResult();
                                }

                            }else {

                                // 不存在，代表前置任务刚刚执行完毕，需要创建 lastTask，最终任务必须在本机执行！
                                TaskDO newLastTask = new TaskDO();
                                newLastTask.setTaskName(TaskConstant.LAST_TASK_NAME);
                                newLastTask.setTaskId(LAST_TASK_ID);
                                newLastTask.setSubInstanceId(instanceId);
                                newLastTask.setAddress(workerRuntime.getWorkerAddress());
                                submitTask(Lists.newArrayList(newLastTask));
                            }
                    }
                }
            }

            // 3. 检查任务实例整体是否超时
            if (isTimeout()) {
                finished.set(true);
                success = false;
                result = SystemInstanceResult.INSTANCE_EXECUTE_TIMEOUT;
            }

            String serverPath = AkkaUtils.getServerActorPath(workerRuntime.getServerDiscoveryService().getCurrentServerAddress());
            ActorSelection serverActor = workerRuntime.getActorSystem().actorSelection(serverPath);

            // 4. 执行完毕，报告服务器
            if (finished.get()) {

                req.setResult(result);
                // 上报追加的工作流上下文信息
                req.setAppendedWfContext(appendedWfContext);
                req.setInstanceStatus(success ? InstanceStatus.SUCCEED.getV() : InstanceStatus.FAILED.getV());

                CompletionStage<Object> askCS = Patterns.ask(serverActor, req, Duration.ofMillis(RemoteConstant.DEFAULT_TIMEOUT_MS));

                boolean serverAccepted = false;
                try {
                    AskResponse askResponse = (AskResponse) askCS.toCompletableFuture().get(RemoteConstant.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    serverAccepted = askResponse.isSuccess();
                }catch (Exception e) {
                    log.warn("[TaskTracker-{}] report finished status failed, result={}.", instanceId, result, e);
                }

                // 服务器未接受上报，则等待下次重新上报
                if (!serverAccepted) {
                    if (++reportFailedCnt > MAX_REPORT_FAILED_THRESHOLD) {
                        log.error("[TaskTracker-{}] try to report finished status(success={}, result={}) lots of times but all failed, it's time to give up, so the process result will be dropped", instanceId, success, result);
                        destroy();
                    }
                    return;
                }

                // 服务器已经更新状态，任务已经执行完毕，开始释放所有资源
                log.info("[TaskTracker-{}] instance process finished,result = {}, start to release resource...", instanceId, result);

                destroy();
                return;
            }

            // 5. 未完成，上报状态
            req.setInstanceStatus(InstanceStatus.RUNNING.getV());
            serverActor.tell(req, null);

            // 6.1 定期检查 -> 重试派发后未确认的任务
            long currentMS = System.currentTimeMillis();
            if (holder.workerUnreceivedNum != 0) {
                taskPersistenceService.getTaskByStatus(instanceId, TaskStatus.DISPATCH_SUCCESS_WORKER_UNCHECK, 100).forEach(uncheckTask -> {

                    long elapsedTime = currentMS - uncheckTask.getLastModifiedTime();
                    if (elapsedTime > DISPATCH_TIME_OUT_MS) {

                        TaskDO updateEntity = new TaskDO();
                        updateEntity.setStatus(TaskStatus.WAITING_DISPATCH.getValue());
                        // 特殊任务只能本机执行
                        if (!TaskConstant.LAST_TASK_NAME.equals(uncheckTask.getTaskName())) {
                            updateEntity.setAddress(RemoteConstant.EMPTY_ADDRESS);
                        }
                        // 失败次数 + 1
                        updateEntity.setFailedCnt(uncheckTask.getFailedCnt() + 1);

                        taskPersistenceService.updateTask(instanceId, uncheckTask.getTaskId(), updateEntity);

                        log.warn("[TaskTracker-{}] task(id={},name={}) try to dispatch again due to unreceived the response from ProcessorTracker.",
                                instanceId, uncheckTask.getTaskId(), uncheckTask.getTaskName());
                    }

                });
            }

            // 6.2 定期检查 -> 重新执行被派发到宕机ProcessorTracker上的任务
            List<String> disconnectedPTs = ptStatusHolder.getAllDisconnectedProcessorTrackers();
            if (!disconnectedPTs.isEmpty()) {
                log.warn("[TaskTracker-{}] some ProcessorTracker disconnected from TaskTracker,their address is {}.", instanceId, disconnectedPTs);
                if (taskPersistenceService.updateLostTasks(instanceId, disconnectedPTs, true)) {
                    ptStatusHolder.remove(disconnectedPTs);
                    log.warn("[TaskTracker-{}] removed these ProcessorTracker from StatusHolder: {}", instanceId, disconnectedPTs);
                }
            }
        }

        @Override
        public void run() {
            try {
                innerRun();
            }catch (Exception e) {
                log.warn("[TaskTracker-{}] status checker execute failed, please fix the bug (@tjq)!", instanceId, e);
            }
        }
    }
}
