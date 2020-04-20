package com.github.kfcfans.oms.worker.core.tracker.processor;

import akka.actor.ActorSelection;
import com.github.kfcfans.common.ExecuteType;
import com.github.kfcfans.common.ProcessorType;
import com.github.kfcfans.common.TimeExpressionType;
import com.github.kfcfans.common.utils.CommonUtils;
import com.github.kfcfans.oms.worker.OhMyWorker;
import com.github.kfcfans.common.RemoteConstant;
import com.github.kfcfans.oms.worker.common.constants.TaskStatus;
import com.github.kfcfans.oms.worker.common.utils.AkkaUtils;
import com.github.kfcfans.oms.worker.common.utils.SpringUtils;
import com.github.kfcfans.oms.worker.core.classloader.ProcessorBeanFactory;
import com.github.kfcfans.oms.worker.core.executor.ProcessorRunnable;
import com.github.kfcfans.oms.worker.core.processor.built.PythonProcessor;
import com.github.kfcfans.oms.worker.core.processor.built.ShellProcessor;
import com.github.kfcfans.oms.worker.persistence.TaskDO;
import com.github.kfcfans.oms.worker.pojo.model.InstanceInfo;
import com.github.kfcfans.oms.worker.pojo.request.ProcessorReportTaskStatusReq;
import com.github.kfcfans.oms.worker.pojo.request.ProcessorTrackerStatusReportReq;
import com.github.kfcfans.oms.worker.pojo.request.TaskTrackerStartTaskReq;
import com.github.kfcfans.oms.worker.core.processor.sdk.BasicProcessor;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

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
    // 任务实例信息
    private InstanceInfo instanceInfo;
    // 冗余 instanceId，方便日志
    private Long instanceId;

    // 任务执行器
    private BasicProcessor processor;

    private String taskTrackerAddress;
    private ActorSelection taskTrackerActorRef;

    private ThreadPoolExecutor threadPool;
    private ScheduledExecutorService timingPool;

    private static final int THREAD_POOL_QUEUE_MAX_SIZE = 100;

    // 当 ProcessorTracker 出现根本性错误（比如 Processor 创建失败，所有的任务直接失败）
    private boolean lethal = false;
    private String lethalReason;

    /**
     * 创建 ProcessorTracker（其实就是创建了个执行用的线程池 T_T）
     */
    public ProcessorTracker(TaskTrackerStartTaskReq request) {
        try {
            // 赋值
            this.startTime = System.currentTimeMillis();
            this.instanceInfo = request.getInstanceInfo();
            this.instanceId = request.getInstanceInfo().getInstanceId();
            this.taskTrackerAddress = request.getTaskTrackerAddress();
            String akkaRemotePath = AkkaUtils.getAkkaWorkerPath(taskTrackerAddress, RemoteConstant.Task_TRACKER_ACTOR_NAME);
            this.taskTrackerActorRef = OhMyWorker.actorSystem.actorSelection(akkaRemotePath);

            // 初始化 线程池
            initThreadPool();
            // 初始化 Processor
            initProcessor();
            // 初始化定时任务
            initTimingJob();

            log.info("[ProcessorTracker-{}] ProcessorTracker was successfully created!", instanceId);
        }catch (Exception e) {
            log.warn("[ProcessorTracker-{}] create ProcessorTracker failed, all tasks submitted here will fail.", instanceId, e);
            lethal = true;
            lethalReason = e.toString();
        }
    }

    /**
     * 提交任务到线程池执行
     * 1.0版本：TaskTracker有任务就dispatch，导致 ProcessorTracker 本地可能堆积过多的任务，造成内存压力。为此 ProcessorTracker 在线程
     *         池队列堆积到一定程度时，会将数据持久化到DB，然后通过异步线程定时从数据库中取回任务，重新提交执行。
     *         联动：数据库的SPID设计、TaskStatus段落设计等，全部取消...
     *         last commitId: 341953aceceafec0fbe7c3d9a3e26451656b945e
     * 2.0版本：ProcessorTracker定时向TaskTracker发送心跳消息，心跳消息中包含了当前线程池队列任务个数，TaskTracker根据ProcessorTracker
     *         的状态判断能否继续派发任务。因此，ProcessorTracker本地不会堆积过多任务，故删除 持久化机制 ╥﹏╥...！
     * @param newTask 需要提交到线程池执行的任务
     */
    public void submitTask(TaskDO newTask) {

        // 一旦 ProcessorTracker 出现异常，所有提交到此处的任务直接返回失败，防止形成死锁
        // 死锁分析：TT创建PT，PT创建失败，无法定期汇报心跳，TT长时间未收到PT心跳，认为PT宕机（确实宕机了），无法选择可用的PT再次派发任务，死锁形成，GG斯密达 T_T
        if (lethal) {
            ProcessorReportTaskStatusReq report = new ProcessorReportTaskStatusReq(instanceId, newTask.getTaskId(), TaskStatus.WORKER_PROCESS_FAILED.getValue(), lethalReason, System.currentTimeMillis());
            taskTrackerActorRef.tell(report, null);
            return;
        }

        boolean success = false;
        // 1. 设置值并提交执行
        newTask.setInstanceId(instanceInfo.getInstanceId());
        newTask.setAddress(taskTrackerAddress);

        ProcessorRunnable processorRunnable = new ProcessorRunnable(instanceInfo, taskTrackerActorRef, newTask, processor);
        try {
            threadPool.submit(processorRunnable);
            success = true;
        }catch (RejectedExecutionException ignore) {
            log.warn("[ProcessorTracker-{}] submit task(taskId={},taskName={}) to ThreadPool failed due to ThreadPool has too much task waiting to process, this task will dispatch to other ProcessorTracker.",
                    instanceId, newTask.getTaskId(), newTask.getTaskName());
        }catch (Exception e) {
            log.error("[ProcessorTracker-{}] submit task(taskId={},taskName={}) to ThreadPool failed.", instanceId, newTask.getTaskId(), newTask.getTaskName(), e);
        }

        // 2. 回复接收成功
        if (success) {
            ProcessorReportTaskStatusReq reportReq = new ProcessorReportTaskStatusReq();
            reportReq.setInstanceId(instanceId);
            reportReq.setTaskId(newTask.getTaskId());
            reportReq.setStatus(TaskStatus.WORKER_RECEIVED.getValue());
            reportReq.setReportTime(System.currentTimeMillis());

            taskTrackerActorRef.tell(reportReq, null);

            log.debug("[ProcessorTracker-{}] submit task(taskId={}, taskName={}) success, current queue size: {}.",
                    instanceId, newTask.getTaskId(), newTask.getTaskName(), threadPool.getQueue().size());
        }
    }

    /**
     * 释放资源
     */
    public void destroy() {

        // 1. 关闭执行执行线程池
        CommonUtils.executeIgnoreException(() -> {
            List<Runnable> tasks = threadPool.shutdownNow();
            if (!CollectionUtils.isEmpty(tasks)) {
                log.warn("[ProcessorTracker-{}] shutdown threadPool now and stop {} tasks.", instanceId, tasks.size());
            }
            return null;
        });

        // 2. 去除顶层引用，送入GC世界
        taskTrackerActorRef = null;
        ProcessorTrackerPool.removeProcessorTracker(instanceId);

        log.info("[ProcessorTracker-{}] ProcessorTracker already destroyed!", instanceId);

        // 3. 关闭定时线程池
        CommonUtils.executeIgnoreException(() -> timingPool.shutdownNow());
    }


    /**
     * 初始化线程池
     */
    private void initThreadPool() {

        int poolSize = calThreadPoolSize();
        // 待执行队列，为了防止对内存造成较大压力，内存队列不能太大
        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(THREAD_POOL_QUEUE_MAX_SIZE);
        // 自定义线程池中线程名称
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("oms-processor-pool-%d").build();
        // 拒绝策略：直接抛出异常
        RejectedExecutionHandler rejectionHandler = new ThreadPoolExecutor.AbortPolicy();

        threadPool = new ThreadPoolExecutor(poolSize, poolSize, 60L, TimeUnit.SECONDS, queue, threadFactory, rejectionHandler);

        // 当没有任务执行时，允许销毁核心线程（即线程池最终存活线程个数可能为0）
        threadPool.allowCoreThreadTimeOut(true);
    }

    /**
     * 初始化定时任务
     */
    private void initTimingJob() {

        // 全称 oms-ProcessTracker-TimingPool
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("oms-ProcessorTrackerTimingPool-%d").build();
        timingPool = Executors.newSingleThreadScheduledExecutor(threadFactory);

        timingPool.scheduleAtFixedRate(new CheckerAndReporter(), 0, 10, TimeUnit.SECONDS);
    }


    /**
     * 定时向 TaskTracker 汇报（携带任务执行信息的心跳）
     */
    private class CheckerAndReporter implements Runnable {

        @Override
        public void run() {

            long interval = System.currentTimeMillis() - startTime;
            // 秒级任务的ProcessorTracker不应该关闭
            if (!TimeExpressionType.frequentTypes.contains(instanceInfo.getTimeExpressionType())) {
                if (interval > instanceInfo.getInstanceTimeoutMS()) {
                    log.warn("[ProcessorTracker-{}] detected instance timeout, maybe TaskTracker's destroy request missed, so try to kill self now.", instanceId);
                    destroy();
                    return;
                }
            }

            long waitingNum = threadPool.getQueue().size();
            ProcessorTrackerStatusReportReq req = new ProcessorTrackerStatusReportReq(instanceId, waitingNum);
            taskTrackerActorRef.tell(req, null);
        }

    }

    /**
     * 初始化处理器 Processor
     */
    private void initProcessor() throws Exception {

        ProcessorType processorType = ProcessorType.valueOf(instanceInfo.getProcessorType());
        String processorInfo = instanceInfo.getProcessorInfo();

        switch (processorType) {
            case EMBEDDED_JAVA:
                // 先使用 Spring 加载
                if (SpringUtils.supportSpringBean()) {
                    try {
                        processor = SpringUtils.getBean(processorInfo);
                    }catch (Exception e) {
                        log.warn("[ProcessorRunnable-{}] no spring bean of processor(className={}), reason is {}.", instanceId, processorInfo, e.toString());
                    }
                }
                // 反射加载
                if (processor == null) {
                    processor = ProcessorBeanFactory.getInstance().getLocalProcessor(processorInfo);
                }
                break;
            case SHELL:
                processor = new ShellProcessor(instanceId, processorInfo, instanceInfo.getInstanceTimeoutMS(), threadPool);
                break;
            case PYTHON:
                processor = new PythonProcessor(instanceId, processorInfo, instanceInfo.getInstanceTimeoutMS(), threadPool);
                break;
            default:
                log.warn("[ProcessorRunnable-{}] unknown processor type: {}.", instanceId, processorType);
                throw new IllegalArgumentException("unknown processor type of " + processorType);
        }

        if (processor == null) {
            log.warn("[ProcessorRunnable-{}] fetch Processor(type={},info={}) failed.", instanceId, processorType, processorInfo);
            throw new IllegalArgumentException("fetch Processor failed");
        }
    }

    /**
     * 计算线程池大小
     */
    private int calThreadPoolSize() {
        ExecuteType executeType = ExecuteType.valueOf(instanceInfo.getExecuteType());
        ProcessorType processorType = ProcessorType.valueOf(instanceInfo.getProcessorType());

        if (executeType == ExecuteType.MAP_REDUCE) {
            return instanceInfo.getThreadConcurrency();
        }

        // 脚本类需要三个线程（执行线程、输入流、错误流），分配 N + 1个线程给线程池
        if (processorType == ProcessorType.PYTHON || processorType == ProcessorType.SHELL) {
            return 4;
        }

        return 2;
    }

}
