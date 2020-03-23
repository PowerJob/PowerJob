package com.github.kfcfans.oms.worker.core.tracker.processor;

import akka.actor.ActorRef;
import com.github.kfcfans.oms.worker.common.constants.TaskStatus;
import com.github.kfcfans.oms.worker.core.executor.ProcessorRunnable;
import com.github.kfcfans.oms.worker.persistence.TaskPersistenceService;
import com.github.kfcfans.oms.worker.pojo.request.ProcessorReportTaskStatusReq;
import com.github.kfcfans.oms.worker.pojo.request.TaskTrackerStartTaskReq;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.beans.BeanUtils;

import java.util.concurrent.*;

/**
 * 负责管理 Processor 的执行
 *
 * @author tjq
 * @since 2020/3/20
 */
public class ProcessorTracker {

    private ExecutorService threadPool;

    public ProcessorTracker(int threadConcurrency) {

        // 初始化运行用的线程池
        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(10);
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("oms-processor-pool-%d").build();
        RejectedProcessorHandler rejectHandler = new RejectedProcessorHandler(TaskPersistenceService.INSTANCE);
        threadPool = new ThreadPoolExecutor(threadConcurrency, threadConcurrency, 60L, TimeUnit.SECONDS, queue, threadFactory, rejectHandler);
    }

    public void submitTask(TaskTrackerStartTaskReq newTaskReq, ActorRef taskTrackerActorRef) {

        // 1. 回复接受成功
        ProcessorReportTaskStatusReq reportReq = new ProcessorReportTaskStatusReq();
        BeanUtils.copyProperties(newTaskReq, reportReq);
        reportReq.setStatus(TaskStatus.RECEIVE_SUCCESS.getValue());
        taskTrackerActorRef.tell(reportReq, null);

        // 2. 提交线程池执行
        ProcessorRunnable processorRunnable = new ProcessorRunnable(taskTrackerActorRef, newTaskReq);
        threadPool.submit(processorRunnable);
    }
}
