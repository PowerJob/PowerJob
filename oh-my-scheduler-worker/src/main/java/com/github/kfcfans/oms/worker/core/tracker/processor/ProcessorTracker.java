package com.github.kfcfans.oms.worker.core.tracker.processor;

import akka.actor.ActorSelection;
import com.github.kfcfans.oms.worker.OhMyWorker;
import com.github.kfcfans.oms.worker.common.constants.AkkaConstant;
import com.github.kfcfans.oms.worker.common.constants.TaskStatus;
import com.github.kfcfans.oms.worker.common.utils.AkkaUtils;
import com.github.kfcfans.oms.worker.core.executor.ProcessorRunnable;
import com.github.kfcfans.oms.worker.persistence.TaskDO;
import com.github.kfcfans.oms.worker.persistence.TaskPersistenceService;
import com.github.kfcfans.oms.worker.pojo.request.ProcessorReportTaskStatusReq;
import com.github.kfcfans.oms.worker.pojo.request.TaskTrackerStartTaskReq;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.*;

/**
 * 负责管理 Processor 的执行
 *
 * @author tjq
 * @since 2020/3/20
 */
@Slf4j
public class ProcessorTracker {

    // 记录创建时间
    private long startTime;
    private long jobTimeLimitMS;

    // 记录该 Job 相关信息
    private String instanceId;
    private String executeType;
    private String processorType;
    private String processorInfo;
    private int threadConcurrency;
    private String jobParams;
    private String instanceParams;
    private int maxRetryTimes;

    private String taskTrackerAddress;
    private ActorSelection taskTrackerActorRef;

    private ThreadPoolExecutor threadPool;
    private static final int MAX_QUEUE_SIZE = 20;

    /**
     * 创建 ProcessorTracker（其实就是创建了个执行用的线程池 T_T）
     */
    public ProcessorTracker(TaskTrackerStartTaskReq request) {

        // 赋值
        this.startTime = System.currentTimeMillis();
        this.jobTimeLimitMS = request.getJobTimeLimitMS();
        this.instanceId = request.getInstanceId();
        this.executeType = request.getExecuteType();
        this.processorType = request.getProcessorType();
        this.processorInfo = request.getProcessorInfo();
        this.threadConcurrency = request.getThreadConcurrency();
        this.jobParams = request.getJobParams();
        this.instanceParams = request.getInstanceParams();
        this.maxRetryTimes = request.getMaxRetryTimes();
        this.taskTrackerAddress = request.getTaskTrackerAddress();

        String akkaRemotePath = AkkaUtils.getAkkaRemotePath(taskTrackerAddress, AkkaConstant.Task_TRACKER_ACTOR_NAME);
        this.taskTrackerActorRef = OhMyWorker.actorSystem.actorSelection(akkaRemotePath);

        // 初始化
        initProcessorPool();
        initTimingJob();
    }

    /**
     * 提交任务
     */
    public void submitTask(TaskTrackerStartTaskReq newTaskReq) {

        // 1. 回复接受成功
        ProcessorReportTaskStatusReq reportReq = new ProcessorReportTaskStatusReq();
        BeanUtils.copyProperties(newTaskReq, reportReq);
        reportReq.setStatus(TaskStatus.RECEIVE_SUCCESS.getValue());
        taskTrackerActorRef.tell(reportReq, null);

        // 2.1 内存控制，持久化
        if (threadPool.getQueue().size() > MAX_QUEUE_SIZE) {

            TaskDO newTask = new TaskDO();
            BeanUtils.copyProperties(newTaskReq, newTask);
            newTask.setTaskContent(newTaskReq.getSubTaskContent());
            newTask.setAddress(newTaskReq.getTaskTrackerAddress());
            newTask.setStatus(TaskStatus.RECEIVE_SUCCESS.getValue());
            newTask.setFailedCnt(newTaskReq.getCurrentRetryTimes());
            newTask.setCreatedTime(System.currentTimeMillis());
            newTask.setLastModifiedTime(System.currentTimeMillis());

            boolean save = TaskPersistenceService.INSTANCE.save(newTask);
            if (save) {
                log.debug("[RejectedProcessorHandler] persistent task({}) succeed.", newTask);
            }else {
                log.warn("[RejectedProcessorHandler] persistent task({}) failed.", newTask);
            }
            return;
        }

        // 2.2 提交执行
        ProcessorRunnable processorRunnable = new ProcessorRunnable(taskTrackerActorRef, newTaskReq);
        threadPool.submit(processorRunnable);
    }

    /**
     * 任务是否超时
     */
    public boolean isTimeout() {
        return System.currentTimeMillis() - startTime > jobTimeLimitMS;
    }


    /**
     * 初始化线程池
     */
    private void initProcessorPool() {
        // 待执行队列，为了防止对内存造成较大压力，内存队列不能太大
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
        // 自定义线程池中线程名称
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("oms-processor-pool-%d").build();
        threadPool = new ThreadPoolExecutor(threadConcurrency, threadConcurrency, 60L, TimeUnit.SECONDS, queue, threadFactory);
        // 当没有任务执行时，允许销毁核心线程（即线程池最终存活线程个数可能为0）
        threadPool.allowCoreThreadTimeOut(true);
    }

    /**
     * 初始化定时任务
     */
    private void initTimingJob() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("oms-processor-timing-pool-%d").build();
        ScheduledExecutorService timingPool = Executors.newSingleThreadScheduledExecutor(threadFactory);

        timingPool.scheduleWithFixedDelay(new PoolStatusCheckRunnable(), 60, 10, TimeUnit.SECONDS);
    }


    /**
     * 定期检查线程池运行状态（内存中的任务数量不足，则即使从数据库中获取并提交执行）
     */
    private class PoolStatusCheckRunnable implements Runnable {

        @Override
        public void run() {

            int queueSize = threadPool.getQueue().size();
            if (queueSize >= MAX_QUEUE_SIZE / 2) {
                return;
            }

            TaskPersistenceService taskPersistenceService = TaskPersistenceService.INSTANCE;
            List<TaskDO> taskDOList = taskPersistenceService.getNeedRunTask(instanceId, MAX_QUEUE_SIZE / 2);

            if (CollectionUtils.isEmpty(taskDOList)) {
                return;
            }

            List<String> deletedIds = Lists.newLinkedList();

            log.debug("[ProcessorTracker] timing add task to thread pool.");

            // 提交到线程池执行
            taskDOList.forEach(task -> {
                runTask(task);
                deletedIds.add(task.getTaskId());
            });

            // 删除任务
            taskPersistenceService.batchDelete(instanceId, deletedIds);
        }

        private void runTask(TaskDO task) {
            TaskTrackerStartTaskReq req = new TaskTrackerStartTaskReq();
            BeanUtils.copyProperties(task, req);

            req.setExecuteType(executeType);
            req.setProcessorType(processorType);
            req.setProcessorInfo(processorInfo);
            req.setTaskTrackerAddress(taskTrackerAddress);
            req.setJobParams(jobParams);
            req.setInstanceParams(instanceParams);
            req.setSubTaskContent(task.getTaskContent());
            req.setMaxRetryTimes(maxRetryTimes);
            req.setCurrentRetryTimes(task.getFailedCnt());

            ProcessorRunnable processorRunnable = new ProcessorRunnable(taskTrackerActorRef, req);
            threadPool.submit(processorRunnable);
        }
    }
}
