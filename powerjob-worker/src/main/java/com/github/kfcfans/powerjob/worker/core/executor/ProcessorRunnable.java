package com.github.kfcfans.powerjob.worker.core.executor;

import akka.actor.ActorSelection;
import com.github.kfcfans.powerjob.common.ExecuteType;
import com.github.kfcfans.powerjob.worker.OhMyWorker;
import com.github.kfcfans.powerjob.worker.common.ThreadLocalStore;
import com.github.kfcfans.powerjob.worker.common.constants.TaskConstant;
import com.github.kfcfans.powerjob.worker.common.constants.TaskStatus;
import com.github.kfcfans.powerjob.worker.common.utils.AkkaUtils;
import com.github.kfcfans.powerjob.worker.common.utils.SerializerUtils;
import com.github.kfcfans.powerjob.worker.core.processor.TaskResult;
import com.github.kfcfans.powerjob.worker.log.OmsLogger;
import com.github.kfcfans.powerjob.worker.persistence.TaskDO;
import com.github.kfcfans.powerjob.worker.persistence.TaskPersistenceService;
import com.github.kfcfans.powerjob.worker.pojo.model.InstanceInfo;
import com.github.kfcfans.powerjob.worker.pojo.request.ProcessorReportTaskStatusReq;
import com.github.kfcfans.powerjob.worker.core.processor.ProcessResult;
import com.github.kfcfans.powerjob.worker.core.processor.TaskContext;
import com.github.kfcfans.powerjob.worker.core.processor.sdk.BasicProcessor;
import com.github.kfcfans.powerjob.worker.core.processor.sdk.BroadcastProcessor;
import com.github.kfcfans.powerjob.worker.core.processor.sdk.MapReduceProcessor;
import com.google.common.base.Stopwatch;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Queue;

/**
 * Processor 执行器
 *
 * @author tjq
 * @since 2020/3/23
 */
@Slf4j
@AllArgsConstructor
public class ProcessorRunnable implements Runnable {


    private final InstanceInfo instanceInfo;
    private final ActorSelection taskTrackerActor;
    private final TaskDO task;
    private final BasicProcessor processor;
    private final OmsLogger omsLogger;
    // 类加载器
    private final ClassLoader classLoader;
    // 重试队列，ProcessorTracker 将会定期重新上报处理结果
    private final Queue<ProcessorReportTaskStatusReq> statusReportRetryQueue;

    public void innerRun() throws InterruptedException {

        String taskId = task.getTaskId();
        Long instanceId = task.getInstanceId();

        log.debug("[ProcessorRunnable-{}] start to run task(taskId={}&taskName={})", instanceId, taskId, task.getTaskName());

        // 0. 完成执行上下文准备 & 上报执行信息
        TaskContext taskContext = new TaskContext();
        BeanUtils.copyProperties(task, taskContext);
        taskContext.setJobId(instanceInfo.getJobId());
        taskContext.setMaxRetryTimes(instanceInfo.getTaskRetryNum());
        taskContext.setCurrentRetryTimes(task.getFailedCnt());
        taskContext.setJobParams(instanceInfo.getJobParams());
        taskContext.setInstanceParams(instanceInfo.getInstanceParams());
        taskContext.setOmsLogger(omsLogger);
        if (task.getTaskContent() != null && task.getTaskContent().length > 0) {
            taskContext.setSubTask(SerializerUtils.deSerialized(task.getTaskContent()));
        }
        taskContext.setUserContext(OhMyWorker.getConfig().getUserContext());
        ThreadLocalStore.setTask(task);

        reportStatus(TaskStatus.WORKER_PROCESSING, null, null);

        // 1. 根任务特殊处理
        ProcessResult processResult;
        ExecuteType executeType = ExecuteType.valueOf(instanceInfo.getExecuteType());
        if (TaskConstant.ROOT_TASK_NAME.equals(task.getTaskName())) {

            // 广播执行：先选本机执行 preProcess，完成后TaskTracker再为所有Worker生成子Task
            if (executeType == ExecuteType.BROADCAST) {

                if (processor instanceof BroadcastProcessor) {

                    BroadcastProcessor broadcastProcessor = (BroadcastProcessor) processor;
                    try {
                        processResult = broadcastProcessor.preProcess(taskContext);
                    }catch (Throwable e) {
                        log.warn("[ProcessorRunnable-{}] broadcast task preProcess failed.", instanceId, e);
                        processResult = new ProcessResult(false, e.toString());
                    }

                }else {
                    processResult = new ProcessResult(true, "NO_PREPOST_TASK");
                }

                reportStatus(processResult.isSuccess() ? TaskStatus.WORKER_PROCESS_SUCCESS : TaskStatus.WORKER_PROCESS_FAILED, suit(processResult.getMsg()), ProcessorReportTaskStatusReq.BROADCAST);
                // 广播执行的第一个 task 只执行 preProcess 部分
                return;
            }
        }

        // 2. 最终任务特殊处理（一定和 TaskTracker 处于相同的机器）
        if (TaskConstant.LAST_TASK_NAME.equals(task.getTaskName())) {

            Stopwatch stopwatch = Stopwatch.createStarted();
            log.debug("[ProcessorRunnable-{}] the last task(taskId={}) start to process.", instanceId, taskId);

            List<TaskResult> taskResults = TaskPersistenceService.INSTANCE.getAllTaskResult(instanceId, task.getSubInstanceId());
            try {
                switch (executeType) {
                    case BROADCAST:

                        if (processor instanceof  BroadcastProcessor) {
                            BroadcastProcessor broadcastProcessor = (BroadcastProcessor) processor;
                            processResult = broadcastProcessor.postProcess(taskContext, taskResults);
                        }else {
                            processResult = BroadcastProcessor.defaultResult(taskResults);
                        }
                        break;
                    case MAP_REDUCE:

                        if (processor instanceof MapReduceProcessor) {
                            MapReduceProcessor mapReduceProcessor = (MapReduceProcessor) processor;
                            processResult = mapReduceProcessor.reduce(taskContext, taskResults);
                        }else {
                            processResult = new ProcessResult(false, "not implement the MapReduceProcessor");
                        }
                        break;
                    default:
                        processResult = new ProcessResult(false, "IMPOSSIBLE OR BUG");
                }
            }catch (Throwable e) {
                processResult = new ProcessResult(false, e.toString());
                log.warn("[ProcessorRunnable-{}] execute last task(taskId={}) failed.", instanceId, taskId, e);
            }

            TaskStatus status = processResult.isSuccess() ? TaskStatus.WORKER_PROCESS_SUCCESS : TaskStatus.WORKER_PROCESS_FAILED;
            reportStatus(status, suit(processResult.getMsg()), null);

            log.info("[ProcessorRunnable-{}] the last task execute successfully, using time: {}", instanceId, stopwatch);
            return;
        }


        // 3. 正式提交运行
        try {
            processResult = processor.process(taskContext);
        }catch (Throwable e) {
            log.warn("[ProcessorRunnable-{}] task(id={},name={}) process failed.", instanceId, taskContext.getTaskId(), taskContext.getTaskName(), e);
            processResult = new ProcessResult(false, e.toString());
        }
        reportStatus(processResult.isSuccess() ? TaskStatus.WORKER_PROCESS_SUCCESS : TaskStatus.WORKER_PROCESS_FAILED, suit(processResult.getMsg()), null);
    }

    /**
     * 上报状态给 TaskTracker
     * @param status Task状态
     * @param result 执行结果，只有结束时才存在
     * @param cmd 特殊需求，比如广播执行需要创建广播任务
     */
    private void reportStatus(TaskStatus status, String result, Integer cmd) {
        ProcessorReportTaskStatusReq req = new ProcessorReportTaskStatusReq();

        req.setInstanceId(task.getInstanceId());
        req.setSubInstanceId(task.getSubInstanceId());
        req.setTaskId(task.getTaskId());
        req.setStatus(status.getValue());
        req.setResult(result);
        req.setReportTime(System.currentTimeMillis());
        req.setCmd(cmd);

        // 最终结束状态要求可靠发送
        if (TaskStatus.finishedStatus.contains(status.getValue())) {
            boolean success = AkkaUtils.reliableTransmit(taskTrackerActor, req);
            if (!success) {
                // 插入重试队列，等待重试
                statusReportRetryQueue.add(req);
                log.warn("[ProcessorRunnable-{}] report task(id={},status={},result={}) failed, will retry later", task.getInstanceId(), task.getTaskId(), status, result);
            }
        } else {
            taskTrackerActor.tell(req, null);
        }
    }

    @Override
    public void run() {
        // 切换线程上下文类加载器（否则用的是 Worker 类加载器，不存在容器类，在序列化/反序列化时会报 ClassNotFoundException）
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            innerRun();
        }catch (InterruptedException ignore) {
        }catch (Throwable e) {
            reportStatus(TaskStatus.WORKER_PROCESS_FAILED, e.toString(), null);
            log.error("[ProcessorRunnable-{}] execute failed, please contact the author(@KFCFans) to fix the bug!", task.getInstanceId(), e);
        }finally {
            ThreadLocalStore.clear();
        }
    }

    // 裁剪返回结果到合适的大小
    private String suit(String result) {

        if (StringUtils.isEmpty(result)) {
            return "";
        }
        final int maxLength = OhMyWorker.getConfig().getMaxResultLength();
        if (result.length() <= maxLength) {
            return result;
        }
        log.warn("[ProcessorRunnable-{}] task(taskId={})'s result is too large({}>{}), a part will be discarded.",
                task.getInstanceId(), task.getTaskId(), result.length(), maxLength);
        return result.substring(0, maxLength).concat("...");
    }
}
