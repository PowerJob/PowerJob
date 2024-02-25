package tech.powerjob.worker.core.processor.runnable;

import com.google.common.base.Stopwatch;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import tech.powerjob.common.enums.ExecuteType;
import tech.powerjob.common.serialize.SerializerUtils;
import tech.powerjob.worker.common.ThreadLocalStore;
import tech.powerjob.worker.common.WorkerRuntime;
import tech.powerjob.worker.common.constants.TaskConstant;
import tech.powerjob.worker.common.constants.TaskStatus;
import tech.powerjob.worker.common.utils.TransportUtils;
import tech.powerjob.worker.common.utils.WorkflowContextUtils;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.TaskResult;
import tech.powerjob.worker.core.processor.WorkflowContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;
import tech.powerjob.worker.core.processor.sdk.BroadcastProcessor;
import tech.powerjob.worker.core.processor.sdk.MapReduceProcessor;
import tech.powerjob.worker.extension.processor.ProcessorBean;
import tech.powerjob.worker.log.OmsLogger;
import tech.powerjob.worker.persistence.PersistenceServiceManager;
import tech.powerjob.worker.persistence.TaskDO;
import tech.powerjob.worker.persistence.TaskPersistenceService;
import tech.powerjob.worker.pojo.model.InstanceInfo;
import tech.powerjob.worker.pojo.request.ProcessorReportTaskStatusReq;

import java.util.*;

/**
 * Processor 执行器
 *
 * @author tjq
 * @author Echo009
 * @since 2020/3/23
 */
@Slf4j
@AllArgsConstructor
@SuppressWarnings("squid:S1181")
public class HeavyProcessorRunnable implements Runnable {


    private final InstanceInfo instanceInfo;
    private final String taskTrackerAddress;
    private final TaskDO task;
    private final ProcessorBean processorBean;
    private final OmsLogger omsLogger;
    /**
     * 重试队列，ProcessorTracker 将会定期重新上报处理结果
     */
    private final Queue<ProcessorReportTaskStatusReq> statusReportRetryQueue;
    private final WorkerRuntime workerRuntime;

    public void innerRun() throws InterruptedException {

        final BasicProcessor processor = processorBean.getProcessor();

        String taskId = task.getTaskId();
        Long instanceId = task.getInstanceId();

        log.debug("[ProcessorRunnable-{}] start to run task(taskId={}&taskName={})", instanceId, taskId, task.getTaskName());
        ThreadLocalStore.setTask(task);
        ThreadLocalStore.setRuntimeMeta(workerRuntime);

        // 0. 构造任务上下文
        WorkflowContext workflowContext = constructWorkflowContext();
        TaskContext taskContext = constructTaskContext();
        taskContext.setWorkflowContext(workflowContext);
        // 1. 上报执行信息
        reportStatus(TaskStatus.WORKER_PROCESSING, null, null, null);

        ProcessResult processResult;
        ExecuteType executeType = ExecuteType.valueOf(instanceInfo.getExecuteType());

        // 2. 根任务 & 广播执行 特殊处理
        if (TaskConstant.ROOT_TASK_NAME.equals(task.getTaskName()) && executeType == ExecuteType.BROADCAST) {
            // 广播执行：先选本机执行 preProcess，完成后 TaskTracker 再为所有 Worker 生成子 Task
            handleBroadcastRootTask(instanceId, taskContext);
            return;
        }

        // 3. 最终任务特殊处理（一定和 TaskTracker 处于相同的机器）
        if (TaskConstant.LAST_TASK_NAME.equals(task.getTaskName())) {
            handleLastTask(taskId, instanceId, taskContext, executeType);
            return;
        }

        // 4. 正式提交运行
        try {
            processResult = processor.process(taskContext);
            if (processResult == null) {
                processResult = new ProcessResult(false, "ProcessResult can't be null");
            }
        } catch (Throwable e) {
            log.warn("[ProcessorRunnable-{}] task(id={},name={}) process failed.", instanceId, taskContext.getTaskId(), taskContext.getTaskName(), e);
            processResult = new ProcessResult(false, e.toString());
        }
        reportStatus(processResult.isSuccess() ? TaskStatus.WORKER_PROCESS_SUCCESS : TaskStatus.WORKER_PROCESS_FAILED, suit(processResult.getMsg()), null, workflowContext.getAppendedContextData());
    }


    private TaskContext constructTaskContext() {
        TaskContext taskContext = new TaskContext();
        taskContext.setJobId(instanceInfo.getJobId());
        taskContext.setInstanceId(task.getInstanceId());
        taskContext.setSubInstanceId(task.getSubInstanceId());
        taskContext.setTaskId(task.getTaskId());
        taskContext.setTaskName(task.getTaskName());
        taskContext.setMaxRetryTimes(instanceInfo.getTaskRetryNum());
        taskContext.setCurrentRetryTimes(task.getFailedCnt());
        taskContext.setJobParams(instanceInfo.getJobParams());
        taskContext.setInstanceParams(instanceInfo.getInstanceParams());
        taskContext.setOmsLogger(omsLogger);
        if (task.getTaskContent() != null && task.getTaskContent().length > 0) {
            taskContext.setSubTask(SerializerUtils.deSerialized(task.getTaskContent()));
        }
        taskContext.setUserContext(workerRuntime.getWorkerConfig().getUserContext());
        return taskContext;
    }

    private WorkflowContext constructWorkflowContext() {
        return new WorkflowContext(instanceInfo.getWfInstanceId(), instanceInfo.getInstanceParams());
    }

    /**
     * 处理最终任务
     * BROADCAST  => {@link BroadcastProcessor#postProcess}
     * MAP_REDUCE => {@link MapReduceProcessor#reduce}
     */
    private void handleLastTask(String taskId, Long instanceId, TaskContext taskContext, ExecuteType executeType) {
        final BasicProcessor processor = processorBean.getProcessor();
        ProcessResult processResult;
        Stopwatch stopwatch = Stopwatch.createStarted();
        log.debug("[ProcessorRunnable-{}] the last task(taskId={}) start to process.", instanceId, taskId);

        TaskPersistenceService taskPersistenceService = Optional.ofNullable(PersistenceServiceManager.fetchTaskPersistenceService(instanceId)).orElse(workerRuntime.getTaskPersistenceService());
        List<TaskResult> taskResults = taskPersistenceService.getAllTaskResult(instanceId, task.getSubInstanceId());
        try {
            switch (executeType) {
                case BROADCAST:

                    if (processor instanceof BroadcastProcessor) {
                        BroadcastProcessor broadcastProcessor = (BroadcastProcessor) processor;
                        processResult = broadcastProcessor.postProcess(taskContext, taskResults);
                    } else {
                        processResult = BroadcastProcessor.defaultResult(taskResults);
                    }
                    break;
                case MAP_REDUCE:

                    if (processor instanceof MapReduceProcessor) {
                        MapReduceProcessor mapReduceProcessor = (MapReduceProcessor) processor;
                        processResult = mapReduceProcessor.reduce(taskContext, taskResults);
                    } else {
                        processResult = new ProcessResult(false, "not implement the MapReduceProcessor");
                    }
                    break;
                default:
                    processResult = new ProcessResult(false, "IMPOSSIBLE OR BUG");
            }
        } catch (Throwable e) {
            processResult = new ProcessResult(false, e.toString());
            log.warn("[ProcessorRunnable-{}] execute last task(taskId={}) failed.", instanceId, taskId, e);
        }

        TaskStatus status = processResult.isSuccess() ? TaskStatus.WORKER_PROCESS_SUCCESS : TaskStatus.WORKER_PROCESS_FAILED;
        reportStatus(status, suit(processResult.getMsg()), null, taskContext.getWorkflowContext().getAppendedContextData());

        log.info("[ProcessorRunnable-{}] the last task execute successfully, using time: {}", instanceId, stopwatch);
    }

    /**
     * 处理广播执行的根任务
     * 即执行 {@link BroadcastProcessor#preProcess}，并通知 TaskerTracker 创建广播子任务
     */
    private void handleBroadcastRootTask(Long instanceId, TaskContext taskContext) {
        BasicProcessor processor = processorBean.getProcessor();
        ProcessResult processResult;
        // 广播执行的第一个 task 只执行 preProcess 部分
        if (processor instanceof BroadcastProcessor) {

            BroadcastProcessor broadcastProcessor = (BroadcastProcessor) processor;
            try {
                processResult = broadcastProcessor.preProcess(taskContext);
            } catch (Throwable e) {
                log.warn("[ProcessorRunnable-{}] broadcast task preProcess failed.", instanceId, e);
                processResult = new ProcessResult(false, e.toString());
            }

        } else {
            processResult = new ProcessResult(true, "NO_PREPOST_TASK");
        }
        // 通知 TaskTracker 创建广播子任务
        reportStatus(processResult.isSuccess() ? TaskStatus.WORKER_PROCESS_SUCCESS : TaskStatus.WORKER_PROCESS_FAILED, suit(processResult.getMsg()), ProcessorReportTaskStatusReq.BROADCAST, taskContext.getWorkflowContext().getAppendedContextData());

    }

    /**
     * 上报状态给 TaskTracker
     *
     * @param status Task状态
     * @param result 执行结果，只有结束时才存在
     * @param cmd    特殊需求，比如广播执行需要创建广播任务
     */
    private void reportStatus(TaskStatus status, String result, Integer cmd, Map<String, String> appendedWfContext) {
        ProcessorReportTaskStatusReq req = new ProcessorReportTaskStatusReq();

        req.setInstanceId(task.getInstanceId());
        req.setSubInstanceId(task.getSubInstanceId());
        req.setTaskId(task.getTaskId());
        req.setStatus(status.getValue());
        req.setResult(result);
        req.setReportTime(System.currentTimeMillis());
        req.setCmd(cmd);
        // 检查追加的上下文大小是否超出限制
        if (instanceInfo.getWfInstanceId() !=null && WorkflowContextUtils.isExceededLengthLimit(appendedWfContext, workerRuntime.getWorkerConfig().getMaxAppendedWfContextLength())) {
            log.warn("[ProcessorRunnable-{}]current length of appended workflow context data is greater than {}, this appended workflow context data will be ignore!",instanceInfo.getInstanceId(), workerRuntime.getWorkerConfig().getMaxAppendedWfContextLength());
            // ignore appended workflow context data
            appendedWfContext = Collections.emptyMap();
        }
        req.setAppendedWfContext(appendedWfContext);

        // 最终结束状态要求可靠发送
        if (TaskStatus.FINISHED_STATUS.contains(status.getValue())) {
            boolean success = TransportUtils.reliablePtReportTask(req, taskTrackerAddress, workerRuntime);
            if (!success) {
                // 插入重试队列，等待重试
                statusReportRetryQueue.add(req);
                log.warn("[ProcessorRunnable-{}] report task(id={},status={},result={}) failed, will retry later", task.getInstanceId(), task.getTaskId(), status, result);
            }
        } else {
            TransportUtils.ptReportTask(req, taskTrackerAddress, workerRuntime);
        }
    }

    @Override
    @SuppressWarnings("squid:S2142")
    public void run() {
        // 切换线程上下文类加载器（否则用的是 Worker 类加载器，不存在容器类，在序列化/反序列化时会报 ClassNotFoundException）
        Thread.currentThread().setContextClassLoader(processorBean.getClassLoader());
        try {
            innerRun();
        } catch (InterruptedException ignore) {
            // ignore
        } catch (Throwable e) {
            reportStatus(TaskStatus.WORKER_PROCESS_FAILED, e.toString(), null, null);
            log.error("[ProcessorRunnable-{}] execute failed, please contact the author(@KFCFans) to fix the bug!", task.getInstanceId(), e);
        } finally {
            ThreadLocalStore.clear();
        }
    }

    /**
     * 裁剪返回结果到合适的大小
     */
    private String suit(String result) {

        if (StringUtils.isEmpty(result)) {
            return "";
        }
        final int maxLength = workerRuntime.getWorkerConfig().getMaxResultLength();
        if (result.length() <= maxLength) {
            return result;
        }
        log.warn("[ProcessorRunnable-{}] task(taskId={})'s result is too large({}>{}), a part will be discarded.",
                task.getInstanceId(), task.getTaskId(), result.length(), maxLength);
        return result.substring(0, maxLength).concat("...");
    }

}
