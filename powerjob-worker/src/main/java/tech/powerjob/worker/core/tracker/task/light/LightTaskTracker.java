package tech.powerjob.worker.core.tracker.task.light;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import tech.powerjob.common.PowerJobDKey;
import tech.powerjob.common.SystemInstanceResult;
import tech.powerjob.common.enums.InstanceStatus;
import tech.powerjob.common.model.InstanceDetail;
import tech.powerjob.common.request.ServerQueryInstanceStatusReq;
import tech.powerjob.common.request.ServerScheduleJobReq;
import tech.powerjob.common.request.TaskTrackerReportInstanceStatusReq;
import tech.powerjob.common.enhance.SafeRunnableWrapper;
import tech.powerjob.worker.common.WorkerRuntime;
import tech.powerjob.worker.common.constants.TaskConstant;
import tech.powerjob.worker.common.constants.TaskStatus;
import tech.powerjob.worker.common.utils.TransportUtils;
import tech.powerjob.worker.core.processor.*;
import tech.powerjob.worker.core.tracker.manager.LightTaskTrackerManager;
import tech.powerjob.worker.core.tracker.task.TaskTracker;
import tech.powerjob.worker.extension.processor.ProcessorBean;
import tech.powerjob.worker.extension.processor.ProcessorDefinition;
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
    private final ProcessorBean processorBean;
    /**
     * 上下文
     */
    private final TaskContext taskContext;
    /**
     * 任务状态
     */
    private TaskStatus status;
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

    private final AtomicBoolean timeoutFlag = new AtomicBoolean(false);

    protected final AtomicBoolean stopFlag = new AtomicBoolean(false);

    protected final AtomicBoolean destroyFlag = new AtomicBoolean(false);


    public LightTaskTracker(ServerScheduleJobReq req, WorkerRuntime workerRuntime) {
        super(req, workerRuntime);
        try {
            taskContext = constructTaskContext(req, workerRuntime);
            // 等待处理
            status = TaskStatus.WORKER_RECEIVED;
            // 加载 Processor
            processorBean = workerRuntime.getProcessorLoader().load(new ProcessorDefinition().setProcessorType(req.getProcessorType()).setProcessorInfo(req.getProcessorInfo()));
            executeThread = new AtomicReference<>();
            long delay = Integer.parseInt(System.getProperty(PowerJobDKey.WORKER_STATUS_CHECK_PERIOD, "15")) * 1000L;
            // 初始延迟加入随机值，避免在高并发场景下所有请求集中在一个时间段
            long initDelay = RandomUtils.nextInt(5000, 10000);
            // 上报任务状态
            statusReportScheduledFuture = workerRuntime.getExecutorManager().getLightweightTaskStatusCheckExecutor().scheduleWithFixedDelay(new SafeRunnableWrapper(this::checkAndReportStatus), initDelay, delay, TimeUnit.MILLISECONDS);
            // 超时控制
            if (instanceInfo.getInstanceTimeoutMS() != Integer.MAX_VALUE) {
                if (instanceInfo.getInstanceTimeoutMS() < 1000L) {
                    timeoutCheckScheduledFuture = workerRuntime.getExecutorManager().getLightweightTaskStatusCheckExecutor().scheduleAtFixedRate(new SafeRunnableWrapper(this::timeoutCheck), instanceInfo.getInstanceTimeoutMS(), instanceInfo.getInstanceTimeoutMS() / 10, TimeUnit.MILLISECONDS);
                } else {
                    // 执行时间超过 1 s 的任务，超时检测最小颗粒度为 1 s
                    timeoutCheckScheduledFuture = workerRuntime.getExecutorManager().getLightweightTaskStatusCheckExecutor().scheduleAtFixedRate(new SafeRunnableWrapper(this::timeoutCheck), instanceInfo.getInstanceTimeoutMS(), 1000L, TimeUnit.MILLISECONDS);
                }
            } else {
                timeoutCheckScheduledFuture = null;
            }
            // 提交任务到线程池
            processFuture = workerRuntime.getExecutorManager().getLightweightTaskExecutorService().submit(this::processTask);
        } catch (Exception e) {
            log.error("[TaskTracker-{}] fail to create TaskTracker for req:{} ", instanceId, req);
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
        if (!destroyFlag.compareAndSet(false, true)) {
            log.warn("[TaskTracker-{}] This TaskTracker has been destroyed!", instanceId);
            return;
        }
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
        log.info("[TaskTracker-{}] remove TaskTracker,task status {},start time:{},end time:{},real cost:{},total time:{}", instanceId, status, taskStartTime, taskEndTime, taskEndTime != null ? taskEndTime - taskStartTime : "unknown", System.currentTimeMillis() - createTime);
    }

    @Override
    public void stopTask() {

        // 已经执行完成，忽略
        if (finished.get()) {
            log.warn("[TaskTracker-{}] fail to stop task,task is finished!result:{}", instanceId, result);
            return;
        }
        if (!stopFlag.compareAndSet(false, true)) {
            log.warn("[TaskTracker-{}] task has been mark as stopped,ignore this request!", instanceId);
            return;
        }
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
    public InstanceDetail fetchRunningStatus(ServerQueryInstanceStatusReq req) {
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
            Thread.currentThread().setContextClassLoader(processorBean.getClassLoader());
            if (res != null && !res.isSuccess()) {
                // 重试
                taskContext.setCurrentRetryTimes(taskContext.getCurrentRetryTimes() + 1);
                log.warn("[TaskTracker-{}] process failed, TaskTracker will have a retry,current retryTimes : {}", instanceId, taskContext.getCurrentRetryTimes());
            }
            try {
                res = processorBean.getProcessor().process(taskContext);
            } catch (InterruptedException e) {
                log.warn("[TaskTracker-{}] task has been interrupted !", instanceId, e);
                Thread.currentThread().interrupt();
                if (timeoutFlag.get()) {
                    res = new ProcessResult(false, SystemInstanceResult.INSTANCE_EXECUTE_TIMEOUT_INTERRUPTED);
                } else if (stopFlag.get()) {
                    res = new ProcessResult(false, SystemInstanceResult.USER_STOP_INSTANCE_INTERRUPTED);
                } else {
                    res = new ProcessResult(false, e.toString());
                }
            } catch (Exception e) {
                log.warn("[TaskTracker-{}] process failed !", instanceId, e);
                res = new ProcessResult(false, e.toString());
            }
            if (res == null) {
                log.warn("[TaskTracker-{}] processor return null !", instanceId);
                res = new ProcessResult(false, "Processor return null");
            }
        } while (!res.isSuccess() && taskContext.getCurrentRetryTimes() < taskContext.getMaxRetryTimes() && !timeoutFlag.get() && !stopFlag.get());
        executeThread.set(null);
        taskEndTime = System.currentTimeMillis();
        finished.set(true);
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


    private synchronized void checkAndReportStatus() {
        if (destroyFlag.get()) {
            // 已经被销毁，不需要上报状态
            log.info("[TaskTracker-{}] has been destroyed,final status is {},needn't to report status!", instanceId, status);
            return;
        }
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
            if (finished.get()) {
                // 已经被成功打断
                destroy();
                return;
            }
            final Thread workerThread = executeThread.get();
            if (!finished.get() && workerThread != null) {
                // 未能成功打断任务，强制停止
                try {
                    if (tryForceStopThread(workerThread)) {
                        finished.set(true);
                        taskEndTime = System.currentTimeMillis();
                        result = new ProcessResult(false, SystemInstanceResult.USER_STOP_INSTANCE_FORCE_STOP);
                        log.warn("[TaskTracker-{}] task need stop, force stop thread {} success!", instanceId, workerThread.getName());
                        // 被终止的任务不需要上报状态
                        destroy();
                        return;
                    }
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
            reportFinalStatusThenDestroy(workerRuntime, reportInstanceStatusReq);
            return;
        }
        // 未完成的任务，只需要上报状态
        reportInstanceStatusReq.setInstanceStatus(InstanceStatus.RUNNING.getV());
        log.info("[TaskTracker-{}] report status({}) success,real status is {}", instanceId, reportInstanceStatusReq, status);
        TransportUtils.ttReportInstanceStatus(reportInstanceStatusReq, workerRuntime.getServerDiscoveryService().getCurrentServerAddress(), workerRuntime.getTransporter());
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
        if (timeoutFlag.compareAndSet(false, true)) {
            // 超时，仅尝试打断任务
            log.warn("[TaskTracker-{}] task timeout,taskStarTime:{},currentTime:{},runningTimeLimit:{}, try to interrupt it.", instanceId, taskStartTime, System.currentTimeMillis(), instanceInfo.getInstanceTimeoutMS());
            processFuture.cancel(true);
            return;
        }
        if (finished.get()) {
            // 已经成功被打断
            log.warn("[TaskTracker-{}] task timeout,taskStarTime:{},endTime:{}, interrupt success.", instanceId, taskStartTime, taskEndTime);
            return;
        }
        Thread workerThread = executeThread.get();
        if (workerThread == null) {
            return;
        }
        // 未能成功打断任务，强制终止
        try {
            if (tryForceStopThread(workerThread)) {
                finished.set(true);
                taskEndTime = System.currentTimeMillis();
                result = new ProcessResult(false, SystemInstanceResult.INSTANCE_EXECUTE_TIMEOUT_FORCE_STOP);
                log.warn("[TaskTracker-{}] task timeout, force stop thread {} success!", instanceId, workerThread.getName());
            }
        } catch (Exception e) {
            log.warn("[TaskTracker-{}] task timeout,fail to stop thread {}", instanceId, workerThread.getName(), e);
        }
    }

    private TaskContext constructTaskContext(ServerScheduleJobReq req, WorkerRuntime workerRuntime) {
        final TaskContext context = new TaskContext();
        context.setTaskId(req.getJobId() + "#" + req.getInstanceId());
        context.setJobId(req.getJobId());
        context.setJobParams(req.getJobParams());
        context.setInstanceId(req.getInstanceId());
        context.setInstanceParams(req.getInstanceParams());
        context.setWorkflowContext(new WorkflowContext(req.getWfInstanceId(), req.getInstanceParams()));
        context.setOmsLogger(OmsLoggerFactory.build(req.getInstanceId(), req.getLogConfig(), workerRuntime));
        context.setTaskName(TaskConstant.ROOT_TASK_NAME);
        context.setMaxRetryTimes(req.getTaskRetryNum());
        context.setCurrentRetryTimes(0);
        context.setUserContext(workerRuntime.getWorkerConfig().getUserContext());
        // 轻量级任务不会涉及到任务分片的处理，不需要处理子任务相关的信息
        return context;
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

    /**
     * try force stop thread
     *
     * @param thread thread
     * @return stop result
     */
    private boolean tryForceStopThread(Thread thread) {

        String threadName = thread.getName();

        String allowStopThread = System.getProperty(PowerJobDKey.WORKER_ALLOWED_FORCE_STOP_THREAD);
        if (!StringUtils.equalsIgnoreCase(allowStopThread, Boolean.TRUE.toString())) {
            log.warn("[TaskTracker-{}] PowerJob not allowed to force stop a thread by config", instanceId);
            return false;
        }

        log.warn("[TaskTracker-{}] fail to interrupt the thread[{}], try to force stop.", instanceId, threadName);
        try {
            thread.stop();
            return true;
        } catch (Throwable t) {
            log.warn("[TaskTracker-{}] stop thread[{}] failed, msg: {}", instanceId, threadName, t.getMessage());
        }
        return false;
    }

}
