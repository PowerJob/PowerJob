package tech.powerjob.worker.core.tracker.task.light;

import akka.actor.ActorSelection;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import tech.powerjob.common.PowerJobDKey;
import tech.powerjob.common.SystemInstanceResult;
import tech.powerjob.common.enums.InstanceStatus;
import tech.powerjob.common.model.InstanceDetail;
import tech.powerjob.common.request.ServerScheduleJobReq;
import tech.powerjob.common.request.TaskTrackerReportInstanceStatusReq;
import tech.powerjob.worker.common.WorkerRuntime;
import tech.powerjob.worker.common.constants.TaskConstant;
import tech.powerjob.worker.common.constants.TaskStatus;
import tech.powerjob.worker.common.utils.AkkaUtils;
import tech.powerjob.worker.core.processor.*;
import tech.powerjob.worker.core.tracker.manager.LightTaskTrackerManager;
import tech.powerjob.worker.core.tracker.task.TaskTracker;
import tech.powerjob.worker.log.OmsLoggerFactory;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Echo009
 * @since 2022/9/19
 */
@Slf4j
public class LightTaskTracker extends TaskTracker {
    /**
     * statusReportScheduledFuture
     */
    private final ScheduledFuture<?> statusReportScheduledFuture;
    /**
     * timeoutCheckScheduledFuture
     */
    private final ScheduledFuture<?> timeoutCheckScheduledFuture;
    /**
     * processFuture
     */
    private final Future<ProcessResult> processFuture;
    /**
     * 执行线程
     */
    private final AtomicReference<Thread> executeThread;
    /**
     * 处理器信息
     */
    private final ProcessorInfo processorInfo;
    /**
     * 上下文
     */
    private final TaskContext taskContext;
    /**
     * 任务状态
     */
    private TaskStatus status;
    /**
     * 创建时间
     */
    private final Long createTime;
    /**
     * 任务开始执行的时间
     */
    private Long taskStartTime;
    /**
     * 任务执行结束的时间 或者 任务被 kill 掉的时间
     */
    private Long taskEndTime;
    /**
     * 任务处理结果
     */
    private ProcessResult result;

    private boolean timeoutFlag = false;

    protected final AtomicBoolean stopFlag = new AtomicBoolean(false);


    public LightTaskTracker(ServerScheduleJobReq req, WorkerRuntime workerRuntime) {
        super(req, workerRuntime);
        try {
            taskContext = constructTaskContext(req, workerRuntime);
            createTime = System.currentTimeMillis();
            // 等待处理
            status = TaskStatus.WORKER_RECEIVED;
            // 加载 Processor
            processorInfo = ProcessorLoader.loadProcessor(workerRuntime, req.getProcessorType(), req.getProcessorInfo());
            executeThread = new AtomicReference<>();
            long delay = Integer.parseInt(System.getProperty(PowerJobDKey.WORKER_STATUS_CHECK_PERIOD, "15")) * 1000L;
            // 初始延迟加入随机值，避免在高并发场景下所有请求集中在一个时间段
            long initDelay = RandomUtils.nextInt(5000, 10000);
            // 上报任务状态
            statusReportScheduledFuture = workerRuntime.getExecutorManager().getLightweightTaskStatusCheckExecutor().scheduleWithFixedDelay(this::checkAndReportStatus, initDelay, delay, TimeUnit.MILLISECONDS);
            // 超时控制
            if (instanceInfo.getInstanceTimeoutMS() != Integer.MAX_VALUE) {
                // 超时控制的最小颗粒度为 1 s
                timeoutCheckScheduledFuture = workerRuntime.getExecutorManager().getLightweightTaskStatusCheckExecutor().scheduleAtFixedRate(this::timeoutCheck, instanceInfo.getInstanceTimeoutMS(), 1000, TimeUnit.MILLISECONDS);
            } else {
                timeoutCheckScheduledFuture = null;
            }
            // 提交任务到线程池
            processFuture = workerRuntime.getExecutorManager().getLightweightTaskExecutorService().submit(this::processTask);
        } catch (Exception e) {
            log.warn("[TaskTracker-{}] fail to create TaskTracker for req:{} ", instanceId, req);
            destroy();
            throw e;
        }

    }

    /**
     * 静态方法创建 TaskTracker
     *
     * @param req 服务端调度任务请求
     * @return LightTaskTracker
     */
    public static LightTaskTracker create(ServerScheduleJobReq req, WorkerRuntime workerRuntime) {
        try {
            return new LightTaskTracker(req, workerRuntime);
        } catch (Exception e) {
            reportCreateErrorToServer(req, workerRuntime, e);
        }
        return null;
    }


    @Override
    public void destroy() {
        if (statusReportScheduledFuture != null) {
            statusReportScheduledFuture.cancel(true);
        }
        if (timeoutCheckScheduledFuture != null) {
            timeoutCheckScheduledFuture.cancel(true);
        }
        if (processFuture != null) {
            processFuture.cancel(true);
        }
        LightTaskTrackerManager.removeTaskTracker(instanceId);
        // 最后一列为总耗时（即占用资源的耗时，当前时间减去创建时间）
        log.warn("[TaskTracker-{}] remove TaskTracker,task status {},start time:{},end time:{},real cost:{},total time:{}", instanceId, status, taskStartTime, taskEndTime, taskEndTime - taskStartTime, System.currentTimeMillis() - createTime);
    }

    @Override
    public void stopTask() {

        // 已经执行完成，忽略
        if (finished.get()) {
            log.warn("[TaskTracker-{}] fail to stop task,task is finished!result:{}", instanceId, result);
            return;
        }
        if (stopFlag.get()) {
            log.warn("[TaskTracker-{}] task has been mark as stopped,ignore this request!", instanceId);
            return;
        }
        stopFlag.set(true);
        // 当前任务尚未执行
        if (status == TaskStatus.WORKER_RECEIVED) {
            log.warn("[TaskTracker-{}] task is not started,destroy this taskTracker directly!", instanceId);
            destroy();
            return;
        }
        // 正在执行
        if (processFuture != null) {
            // 尝试打断
            log.info("[TaskTracker-{}] try to interrupt task!", instanceId);
            processFuture.cancel(true);
        }
    }

    @Override
    public InstanceDetail fetchRunningStatus() {
        InstanceDetail detail = new InstanceDetail();
        // 填充基础信息
        detail.setActualTriggerTime(createTime);
        detail.setStatus(InstanceStatus.RUNNING.getV());
        detail.setTaskTrackerAddress(workerRuntime.getWorkerAddress());
        // 填充详细信息
        InstanceDetail.TaskDetail taskDetail = new InstanceDetail.TaskDetail();
        taskDetail.setSucceedTaskNum(0);
        taskDetail.setFailedTaskNum(0);
        taskDetail.setTotalTaskNum(1);
        detail.setTaskDetail(taskDetail);
        return detail;
    }

    private ProcessResult processTask() {
        executeThread.set(Thread.currentThread());
        // 设置任务开始执行的时间
        taskStartTime = System.currentTimeMillis();
        status = TaskStatus.WORKER_PROCESSING;
        // 开始执行时，提交任务判断是否超时
        ProcessResult res = null;
        do {
            Thread.currentThread().setContextClassLoader(processorInfo.getClassLoader());
            if (res != null && !res.isSuccess()) {
                // 重试
                taskContext.setCurrentRetryTimes(taskContext.getCurrentRetryTimes() + 1);
                log.warn("[TaskTracker-{}] process failed, TaskTracker will have a retry,current retryTimes : {}", instanceId, taskContext.getCurrentRetryTimes());
            }
            try {
                res = processorInfo.getBasicProcessor().process(taskContext);
            } catch (InterruptedException e) {
                log.warn("[TaskTracker-{}] task has been interrupted !", instanceId, e);
                res = new ProcessResult(false, e.toString());
            } catch (Exception e) {
                log.warn("[TaskTracker-{}] process failed !", instanceId, e);
                res = new ProcessResult(false, e.toString());
            }
            if (res == null) {
                log.warn("[TaskTracker-{}] processor return null !", instanceId);
                res = new ProcessResult(false, "Processor return null");
            }
        } while (!res.isSuccess() && taskContext.getCurrentRetryTimes() < taskContext.getMaxRetryTimes() && !timeoutFlag);
        executeThread.set(null);
        taskEndTime = System.currentTimeMillis();
        finished.set(true);
        // 成功的情况下允许覆盖超时控制赋予的结果值
        if (timeoutFlag && !res.isSuccess()) {
            return res;
        }
        result = res;
        status = result.isSuccess() ? TaskStatus.WORKER_PROCESS_SUCCESS : TaskStatus.WORKER_PROCESS_FAILED;
        // 取消超时检查任务
        if (timeoutCheckScheduledFuture != null) {
            timeoutCheckScheduledFuture.cancel(true);
        }
        log.info("[TaskTracker-{}] task complete ! create time:{},queue time:{},use time:{},result:{}", instanceId, createTime, taskStartTime - createTime, System.currentTimeMillis() - taskStartTime, result);
        // 执行完成后立即上报一次
        checkAndReportStatus();
        return result;
    }


    private void checkAndReportStatus() {
        String serverPath = AkkaUtils.getServerActorPath(workerRuntime.getServerDiscoveryService().getCurrentServerAddress());
        ActorSelection serverActor = workerRuntime.getActorSystem().actorSelection(serverPath);
        TaskTrackerReportInstanceStatusReq reportInstanceStatusReq = new TaskTrackerReportInstanceStatusReq();
        reportInstanceStatusReq.setAppId(workerRuntime.getAppId());
        reportInstanceStatusReq.setJobId(instanceInfo.getJobId());
        reportInstanceStatusReq.setInstanceId(instanceId);
        reportInstanceStatusReq.setWfInstanceId(instanceInfo.getWfInstanceId());
        reportInstanceStatusReq.setTotalTaskNum(1);
        reportInstanceStatusReq.setReportTime(System.currentTimeMillis());
        reportInstanceStatusReq.setStartTime(createTime);
        reportInstanceStatusReq.setSourceAddress(workerRuntime.getWorkerAddress());
        reportInstanceStatusReq.setSucceedTaskNum(0);
        reportInstanceStatusReq.setFailedTaskNum(0);

        if (stopFlag.get()) {
            final Thread workerThread = executeThread.get();
            if (!finished.get() && workerThread != null) {
                // 未能成功打断任务，强制停止
                log.warn("[TaskTracker-{}] task need stop,but fail to interrupt it,force stop thread {}", instanceId, executeThread.get().getName());
                try {
                    workerThread.stop();
                    finished.set(true);
                    log.warn("[TaskTracker-{}] task need stop, force stop thread {} success!", instanceId, workerThread.getName());
                    // 被终止的任务不需要上报状态
                    destroy();
                } catch (Exception e) {
                    log.warn("[TaskTracker-{}] task need stop,fail to stop thread {}", instanceId, workerThread.getName(), e);
                }
            }
        }
        if (finished.get()) {
            if (result.isSuccess()) {
                reportInstanceStatusReq.setSucceedTaskNum(1);
                reportInstanceStatusReq.setInstanceStatus(InstanceStatus.SUCCEED.getV());
            } else {
                reportInstanceStatusReq.setFailedTaskNum(1);
                reportInstanceStatusReq.setInstanceStatus(InstanceStatus.FAILED.getV());
            }
            // 处理工作流上下文
            if (taskContext.getWorkflowContext().getWfInstanceId() != null) {
                reportInstanceStatusReq.setAppendedWfContext(taskContext.getWorkflowContext().getAppendedContextData());
            }
            reportInstanceStatusReq.setResult(suit(result.getMsg()));
            reportInstanceStatusReq.setEndTime(taskEndTime);
            // 微操一下，上报最终状态时重新设置下时间，并且增加一小段偏移，保证在并发上报运行中状态以及最终状态时，最终状态的上报时间晚于运行中的状态
            reportInstanceStatusReq.setReportTime(System.currentTimeMillis() + 1);
            reportFinalStatus(serverActor, reportInstanceStatusReq);
            return;
        }
        // 未完成的任务，只需要上报状态
        reportInstanceStatusReq.setInstanceStatus(InstanceStatus.RUNNING.getV());
        log.info("[TaskTracker-{}] report status({}) success,real status is {}", instanceId, reportInstanceStatusReq, status);
        serverActor.tell(reportInstanceStatusReq, null);
    }

    private void timeoutCheck() {
        if (taskStartTime == null || System.currentTimeMillis() - taskStartTime < instanceInfo.getInstanceTimeoutMS()) {
            return;
        }
        if (finished.get() && result != null) {
            timeoutCheckScheduledFuture.cancel(true);
            return;
        }
        // 首次判断超时
        if (!timeoutFlag) {
            // 超时，仅尝试打断任务
            log.warn("[TaskTracker-{}] task timeout,taskStarTime:{},currentTime:{},runningTimeLimit:{}, try to interrupt it.", instanceId, taskStartTime, System.currentTimeMillis(),instanceInfo.getInstanceTimeoutMS());
            processFuture.cancel(true);
            timeoutFlag = true;
            return;
        }
        if (finished.get()) {
            // 已经成功被打断
            log.warn("[TaskTracker-{}] task timeout,taskStarTime:{},endTime:{}, interrupt success.", instanceId, taskStartTime, taskEndTime);
            if (result == null) {
                taskEndTime = System.currentTimeMillis();
                result = new ProcessResult(false, SystemInstanceResult.INSTANCE_EXECUTE_TIMEOUT_INTERRUPTED);
            }
            return;
        }
        Thread workerThread = executeThread.get();
        if (workerThread == null) {
            return;
        }
        // 未能成功打断任务，强制终止
        log.warn("[TaskTracker-{}] task timeout,but fail to interrupt it,force stop thread {}", instanceId, workerThread.getName());
        try {
            workerThread.stop();
            finished.set(true);
            taskEndTime = System.currentTimeMillis();
            result = new ProcessResult(false, SystemInstanceResult.INSTANCE_EXECUTE_TIMEOUT_FORCE_STOP);
            log.warn("[TaskTracker-{}] task timeout, force stop thread {} success!", instanceId, workerThread.getName());
        } catch (Exception e) {
            log.warn("[TaskTracker-{}] task timeout,fail to stop thread {}", instanceId, workerThread.getName(), e);
        }
    }

    private TaskContext constructTaskContext(ServerScheduleJobReq req, WorkerRuntime workerRuntime) {
        final TaskContext taskContext = new TaskContext();
        taskContext.setTaskId(req.getJobId() + "#" + req.getInstanceId());
        taskContext.setJobId(req.getJobId());
        taskContext.setJobParams(req.getJobParams());
        taskContext.setInstanceId(req.getInstanceId());
        taskContext.setInstanceParams(req.getInstanceParams());
        taskContext.setWorkflowContext(new WorkflowContext(req.getWfInstanceId(), req.getInstanceParams()));
        taskContext.setOmsLogger(OmsLoggerFactory.build(req.getInstanceId(), req.getLogConfig(), workerRuntime));
        taskContext.setTaskName(TaskConstant.ROOT_TASK_NAME);
        taskContext.setMaxRetryTimes(req.getTaskRetryNum());
        taskContext.setCurrentRetryTimes(0);
        taskContext.setUserContext(workerRuntime.getWorkerConfig().getUserContext());
        // 轻量级任务不会涉及到任务分片的处理，不需要处理子任务相关的信息
        return taskContext;
    }

    private String suit(String result) {
        if (StringUtils.isEmpty(result)) {
            return "";
        }
        final int maxLength = workerRuntime.getWorkerConfig().getMaxResultLength();
        if (result.length() <= maxLength) {
            return result;
        }
        log.warn("[TaskTracker-{}] task's result is too large({}>{}), a part will be discarded.",
                instanceId, result.length(), maxLength);
        return result.substring(0, maxLength).concat("...");
    }

}
