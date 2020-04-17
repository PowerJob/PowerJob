package com.github.kfcfans.oms.worker.core.tracker.task;

import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import com.github.kfcfans.common.ExecuteType;
import com.github.kfcfans.common.InstanceStatus;
import com.github.kfcfans.common.SystemInstanceResult;
import com.github.kfcfans.common.model.InstanceDetail;
import com.github.kfcfans.common.request.ServerScheduleJobReq;
import com.github.kfcfans.common.request.TaskTrackerReportInstanceStatusReq;
import com.github.kfcfans.common.response.AskResponse;
import com.github.kfcfans.oms.worker.OhMyWorker;
import com.github.kfcfans.common.RemoteConstant;
import com.github.kfcfans.oms.worker.common.constants.TaskConstant;
import com.github.kfcfans.oms.worker.common.constants.TaskStatus;
import com.github.kfcfans.oms.worker.common.utils.AkkaUtils;
import com.github.kfcfans.oms.worker.persistence.TaskDO;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * 负责管理 JobInstance 的运行，主要包括任务的派发（MR可能存在大量的任务）和状态的更新
 *
 * @author tjq
 * @since 2020/3/17
 */
@Slf4j
@ToString
public class CommonTaskTracker extends TaskTracker {

    private static final String ROOT_TASK_ID = "0";
    // 可以是除 ROOT_TASK_ID 的任何数字
    private static final String LAST_TASK_ID = "1111";

    protected CommonTaskTracker(ServerScheduleJobReq req) {
        super(req);
    }

    @Override
    protected void initTaskTracker(ServerScheduleJobReq req) {

        ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("oms-TaskTrackerTimingPool-%d").build();
        this.scheduledPool = Executors.newScheduledThreadPool(2, factory);

        // 持久化根任务
        persistenceRootTask();

        // 启动定时任务（任务派发 & 状态检查）
        scheduledPool.scheduleWithFixedDelay(new Dispatcher(), 0, 1, TimeUnit.SECONDS);
        scheduledPool.scheduleWithFixedDelay(new StatusCheckRunnable(), 10, 10, TimeUnit.SECONDS);
    }

    @Override
    public InstanceDetail fetchRunningStatus() {

        InstanceDetail detail = new InstanceDetail();
        // 填充基础信息
        detail.setActualTriggerTime(createTime);
        detail.setStatus(InstanceStatus.RUNNING.getDes());
        detail.setTaskTrackerAddress(OhMyWorker.getWorkerAddress());

        // 填充详细信息
        InstanceStatisticsHolder holder = getInstanceStatisticsHolder(instanceId);
        InstanceDetail.TaskDetail taskDetail = new InstanceDetail.TaskDetail();
        taskDetail.setSucceedTaskNum(holder.succeedNum);
        taskDetail.setFailedTaskNum(holder.failedNum);
        taskDetail.setTotalTaskNum(holder.getTotalTaskNum());
        detail.setExtra(taskDetail);

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
        rootTask.setAddress(OhMyWorker.getWorkerAddress());
        rootTask.setTaskName(TaskConstant.ROOT_TASK_NAME);
        rootTask.setCreatedTime(System.currentTimeMillis());
        rootTask.setLastModifiedTime(System.currentTimeMillis());
        rootTask.setLastReportTime(-1L);
        rootTask.setSubInstanceId(instanceId);

        if (!taskPersistenceService.save(rootTask)) {
            log.error("[TaskTracker-{}] create root task failed.", instanceId);
        }else {
            log.info("[TaskTracker-{}] create root task successfully.", instanceId);
        }
    }


    /**
     * 定时检查当前任务的执行状态
     */
    private class StatusCheckRunnable implements Runnable {

        private static final long TIME_OUT_MS = 5000;

        private void innerRun() {

            InstanceStatisticsHolder holder = getInstanceStatisticsHolder(instanceId);

            long finishedNum = holder.succeedNum + holder.failedNum;
            long unfinishedNum = holder.waitingDispatchNum + holder.workerUnreceivedNum + holder.receivedNum + holder.runningNum;

            log.debug("[TaskTracker-{}] status check result: {}", instanceId, holder);

            TaskTrackerReportInstanceStatusReq req = new TaskTrackerReportInstanceStatusReq();
            req.setJobId(instanceInfo.getJobId());
            req.setInstanceId(instanceId);
            req.setTotalTaskNum(finishedNum + unfinishedNum);
            req.setSucceedTaskNum(holder.succeedNum);
            req.setFailedTaskNum(holder.failedNum);
            req.setReportTime(System.currentTimeMillis());
            req.setStartTime(createTime);
            req.setSourceAddress(OhMyWorker.getWorkerAddress());

            boolean success = false;
            String result = null;

            // 2. 如果未完成任务数为0，判断是否真正结束，并获取真正结束任务的执行结果
            if (unfinishedNum == 0) {

                // 数据库中一个任务都没有，说明根任务创建失败，该任务实例失败
                if (finishedNum == 0) {
                    finished.set(true);
                    success = false;
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
                        // MapReduce 和 Broadcast 任务实例是否完成根据**Last_Task**的执行情况判断
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
                                newLastTask.setAddress(OhMyWorker.getWorkerAddress());
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

            String serverPath = AkkaUtils.getAkkaServerPath(RemoteConstant.SERVER_ACTOR_NAME);
            ActorSelection serverActor = OhMyWorker.actorSystem.actorSelection(serverPath);

            // 4. 执行完毕，报告服务器
            if (finished.get()) {

                req.setResult(result);
                req.setInstanceStatus(success ? InstanceStatus.SUCCEED.getV() : InstanceStatus.FAILED.getV());

                CompletionStage<Object> askCS = Patterns.ask(serverActor, req, Duration.ofMillis(TIME_OUT_MS));

                boolean serverAccepted = false;
                try {
                    AskResponse askResponse = (AskResponse) askCS.toCompletableFuture().get(TIME_OUT_MS, TimeUnit.MILLISECONDS);
                    serverAccepted = askResponse.isSuccess();
                }catch (Exception e) {
                    log.warn("[TaskTracker-{}] report finished status failed, result={}.", instanceId, result);
                }

                // 服务器未接受上报，则等待下次重新上报
                if (!serverAccepted) {
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
                    if (elapsedTime > TIME_OUT_MS) {

                        TaskDO updateEntity = new TaskDO();
                        updateEntity.setStatus(TaskStatus.WAITING_DISPATCH.getValue());
                        // 特殊任务只能本机执行
                        if (!TaskConstant.LAST_TASK_NAME.equals(uncheckTask.getTaskName())) {
                            updateEntity.setAddress(RemoteConstant.EMPTY_ADDRESS);
                        }
                        // 失败次数 + 1
                        updateEntity.setFailedCnt(uncheckTask.getFailedCnt() + 1);

                        taskPersistenceService.updateTask(instanceId, uncheckTask.getTaskId(), updateEntity);

                        log.warn("[TaskTracker-{}] task(taskId={}) try to dispatch again due to unreceived the response from ProcessorTracker.",
                                instanceId, uncheckTask.getTaskId());
                    }

                });
            }

            // 6.2 定期检查 -> 重新执行被派发到宕机ProcessorTracker上的任务
            List<String> disconnectedPTs = ptStatusHolder.getAllDisconnectedProcessorTrackers();
            if (!disconnectedPTs.isEmpty()) {
                log.warn("[TaskTracker-{}] some ProcessorTracker disconnected from TaskTracker,their address is {}.", instanceId, disconnectedPTs);
                taskPersistenceService.updateLostTasks(disconnectedPTs);
            }

            // 6.3 超时检查 -> 检查超时的Task

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
