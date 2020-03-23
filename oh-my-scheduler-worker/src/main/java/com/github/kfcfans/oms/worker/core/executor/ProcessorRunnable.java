package com.github.kfcfans.oms.worker.core.executor;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import com.alibaba.fastjson.JSONObject;
import com.github.kfcfans.common.ExecuteType;
import com.github.kfcfans.common.ProcessorType;
import com.github.kfcfans.oms.worker.common.ThreadLocalStore;
import com.github.kfcfans.oms.worker.common.constants.TaskConstant;
import com.github.kfcfans.oms.worker.common.constants.TaskStatus;
import com.github.kfcfans.oms.worker.common.utils.SpringUtils;
import com.github.kfcfans.oms.worker.core.classloader.ProcessorBeanFactory;
import com.github.kfcfans.oms.worker.pojo.request.BroadcastTaskPreExecuteFinishedReq;
import com.github.kfcfans.oms.worker.pojo.request.TaskTrackerStartTaskReq;
import com.github.kfcfans.oms.worker.pojo.request.ProcessorReportTaskStatusReq;
import com.github.kfcfans.oms.worker.sdk.ProcessResult;
import com.github.kfcfans.oms.worker.sdk.TaskContext;
import com.github.kfcfans.oms.worker.sdk.api.BasicProcessor;
import com.github.kfcfans.oms.worker.sdk.api.BroadcastProcessor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

/**
 * Processor 执行器
 *
 * @author tjq
 * @since 2020/3/23
 */
@Slf4j
@AllArgsConstructor
public class ProcessorRunnable implements Runnable {

    private final ActorSelection taskTrackerActor;

    @Getter
    private final TaskTrackerStartTaskReq request;

    @Override
    public void run() {

        // 0. 创建回复
        ProcessorReportTaskStatusReq reportStatus = new ProcessorReportTaskStatusReq();
        BeanUtils.copyProperties(request, reportStatus);

        // 1. 获取 Processor
        BasicProcessor processor = getProcessor();
        if (processor == null) {
            reportStatus.setStatus(TaskStatus.PROCESS_FAILED.getValue());
            reportStatus.setResult("NO_PROCESSOR");
            taskTrackerActor.tell(reportStatus, null);
            return;
        }

        // 2. 根任务特殊处理
        ExecuteType executeType = ExecuteType.valueOf(request.getExecuteType());
        if (TaskConstant.ROOT_TASK_ID.equals(request.getTaskId())) {

            // 广播执行：先选本机执行 preProcess，完成后TaskTracker再为所有Worker生成子Task
            if (executeType == ExecuteType.BROADCAST) {

                BroadcastProcessor broadcastProcessor = (BroadcastProcessor) processor;
                BroadcastTaskPreExecuteFinishedReq spReq = new BroadcastTaskPreExecuteFinishedReq();
                BeanUtils.copyProperties(request, reportStatus);
                try {
                    ProcessResult processResult = broadcastProcessor.preProcess();
                    spReq.setSuccess(processResult.isSuccess());
                    spReq.setMsg(processResult.getMsg());
                }catch (Exception e) {
                    log.warn("[ProcessorRunnable] broadcast task(jobId={}) preProcess failed.", request.getJobId(), e);
                    spReq.setSuccess(false);
                    spReq.setMsg(e.toString());
                }

                taskTrackerActor.tell(spReq, null);
            }
        }

        // 3. 通知 TaskTracker 任务开始运行
        reportStatus.setStatus(TaskStatus.PROCESSING.getValue());
        taskTrackerActor.tell(reportStatus, null);

        // 4. 完成提交前准备工作
        ProcessResult processResult;
        TaskContext taskContext = new TaskContext();
        BeanUtils.copyProperties(request, taskContext);
        taskContext.setSubTask(JSONObject.parse(request.getSubTaskContent()));

        ThreadLocalStore.TASK_CONTEXT_THREAD_LOCAL.set(taskContext);

        // 5. 正式提交运行
        ProcessorReportTaskStatusReq reportReq = new ProcessorReportTaskStatusReq();
        BeanUtils.copyProperties(request, reportReq);
        try {
            processResult = processor.process(taskContext);
            reportReq.setResult(processResult.getMsg());
            if (processResult.isSuccess()) {
                reportReq.setStatus(TaskStatus.PROCESS_SUCCESS.getValue());
            }else {
                reportReq.setStatus(TaskStatus.PROCESS_FAILED.getValue());
            }
        }catch (Exception e) {
            log.warn("[ProcessorRunnable] task({}) process failed.", taskContext.getDescription(), e);

            reportReq.setResult(e.toString());
            reportReq.setStatus(TaskStatus.PROCESS_FAILED.getValue());
        }
        taskTrackerActor.tell(reportReq, null);
    }

    private BasicProcessor getProcessor() {
        BasicProcessor processor = null;
        ProcessorType processorType = ProcessorType.valueOf(request.getProcessorType());

        String processorInfo = request.getProcessorInfo();

        switch (processorType) {
            case EMBEDDED_JAVA:
                // 先使用 Spring 加载
                if (SpringUtils.supportSpringBean()) {
                    try {
                        processor = SpringUtils.getBean(processorInfo);
                    }catch (Exception e) {
                        log.warn("[ProcessorRunnable] no spring bean of processor(className={}).", processorInfo);
                    }
                }
                // 反射加载
                if (processor == null) {
                    processor = ProcessorBeanFactory.getInstance().getLocalProcessor(processorInfo);
                }
        }

        return processor;
    }
}
