package tech.powerjob.worker.core.tracker.processor;

import akka.actor.ActorSelection;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.util.CollectionUtils;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.RemoteConstant;
import tech.powerjob.common.enums.ExecuteType;
import tech.powerjob.common.enums.ProcessorType;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.worker.common.WorkerRuntime;
import tech.powerjob.worker.common.constants.TaskStatus;
import tech.powerjob.worker.common.utils.AkkaUtils;
import tech.powerjob.worker.common.utils.SpringUtils;
import tech.powerjob.worker.container.OmsContainer;
import tech.powerjob.worker.container.OmsContainerFactory;
import tech.powerjob.worker.core.ProcessorBeanFactory;
import tech.powerjob.worker.core.executor.ProcessorRunnable;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;
import tech.powerjob.worker.log.OmsLogger;
import tech.powerjob.worker.log.impl.OmsServerLogger;
import tech.powerjob.worker.persistence.TaskDO;
import tech.powerjob.worker.pojo.model.InstanceInfo;
import tech.powerjob.worker.pojo.request.ProcessorReportTaskStatusReq;
import tech.powerjob.worker.pojo.request.ProcessorTrackerStatusReportReq;
import tech.powerjob.worker.pojo.request.TaskTrackerStartTaskReq;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * 负责管理 Processor 的执行
 *
 * @author tjq
 * @since 2020/3/20
 */
@Slf4j
public class ProcessorTracker {

    /**
     * 记录创建时间
     */
    private long startTime;
    private WorkerRuntime workerRuntime;
    /**
     * 任务实例信息
     */
    private InstanceInfo instanceInfo;
    /**
     * 冗余 instanceId，方便日志
     */
    private Long instanceId;
    /**
     * 任务执行器
     */
    private BasicProcessor processor;
    /**
     * 容器（可能为空）
     */
    private OmsContainer omsContainer;
    /**
     * 在线日志
     */
    private OmsLogger omsLogger;
    /**
     * ProcessResult 上报失败的重试队列
     */
    private Queue<ProcessorReportTaskStatusReq> statusReportRetryQueue;
    /**
     * 上一次空闲时间（用于闲置判定）
     */
    private long lastIdleTime;
    /**
     * 上次完成任务数量（用于闲置判定）
     */
    private long lastCompletedTaskCount;

    private String taskTrackerAddress;

    private ActorSelection taskTrackerActorRef;

    private ThreadPoolExecutor threadPool;

    private ScheduledExecutorService timingPool;

    private static final int THREAD_POOL_QUEUE_MAX_SIZE = 128;
    /**
     * 长时间空闲的 ProcessorTracker 会发起销毁请求
     */
    private static final long MAX_IDLE_TIME = 120000;
    /**
     * 当 ProcessorTracker 出现根本性错误（比如 Processor 创建失败，所有的任务直接失败）
     */
    private boolean lethal = false;

    private String lethalReason;

    /**
     * 创建 ProcessorTracker（其实就是创建了个执行用的线程池 T_T）
     */
    @SuppressWarnings("squid:S1181")
    public ProcessorTracker(TaskTrackerStartTaskReq request, WorkerRuntime workerRuntime) {
        try {
            // 赋值
            this.startTime = System.currentTimeMillis();
            this.workerRuntime = workerRuntime;
            this.instanceInfo = request.getInstanceInfo();
            this.instanceId = request.getInstanceInfo().getInstanceId();
            this.taskTrackerAddress = request.getTaskTrackerAddress();

            String akkaRemotePath = AkkaUtils.getAkkaWorkerPath(taskTrackerAddress, RemoteConstant.TASK_TRACKER_ACTOR_NAME);
            this.taskTrackerActorRef = workerRuntime.getActorSystem().actorSelection(akkaRemotePath);

            this.omsLogger = new OmsServerLogger(instanceId, workerRuntime.getOmsLogHandler());
            this.statusReportRetryQueue = Queues.newLinkedBlockingQueue();
            this.lastIdleTime = -1L;
            this.lastCompletedTaskCount = 0L;

            // 初始化 线程池，TimingPool 启动的任务会检查 ThreadPool，所以必须先初始化线程池，否则NPE
            initThreadPool();
            // 初始化定时任务
            initTimingJob();
            // 初始化 Processor
            initProcessor();

            log.info("[ProcessorTracker-{}] ProcessorTracker was successfully created!", instanceId);
        } catch (Throwable t) {
            log.warn("[ProcessorTracker-{}] create ProcessorTracker failed, all tasks submitted here will fail.", instanceId, t);
            lethal = true;
            lethalReason = ExceptionUtils.getMessage(t);
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
            ProcessorReportTaskStatusReq report = new ProcessorReportTaskStatusReq()
                    .setInstanceId(instanceId)
                    .setSubInstanceId(newTask.getSubInstanceId())
                    .setTaskId(newTask.getTaskId())
                    .setStatus(TaskStatus.WORKER_PROCESS_FAILED.getValue())
                    .setResult(lethalReason)
                    .setReportTime(System.currentTimeMillis());

            taskTrackerActorRef.tell(report, null);
            return;
        }

        boolean success = false;
        // 1. 设置值并提交执行
        newTask.setInstanceId(instanceInfo.getInstanceId());
        newTask.setAddress(taskTrackerAddress);

        ClassLoader classLoader = omsContainer == null ? getClass().getClassLoader() : omsContainer.getContainerClassLoader();
        ProcessorRunnable processorRunnable = new ProcessorRunnable(instanceInfo, taskTrackerActorRef, newTask, processor, omsLogger, classLoader, statusReportRetryQueue, workerRuntime);
        try {
            threadPool.submit(processorRunnable);
            success = true;
        } catch (RejectedExecutionException ignore) {
            log.warn("[ProcessorTracker-{}] submit task(taskId={},taskName={}) to ThreadPool failed due to ThreadPool has too much task waiting to process, this task will dispatch to other ProcessorTracker.",
                    instanceId, newTask.getTaskId(), newTask.getTaskName());
        } catch (Exception e) {
            log.error("[ProcessorTracker-{}] submit task(taskId={},taskName={}) to ThreadPool failed.", instanceId, newTask.getTaskId(), newTask.getTaskName(), e);
        }

        // 2. 回复接收成功
        if (success) {
            ProcessorReportTaskStatusReq reportReq = new ProcessorReportTaskStatusReq();
            reportReq.setInstanceId(instanceId);
            reportReq.setSubInstanceId(newTask.getSubInstanceId());
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

        // 0. 移除Container引用
        if (omsContainer != null) {
            omsContainer.tryRelease();
        }

        // 1. 关闭执行执行线程池
        CommonUtils.executeIgnoreException(() -> {
            List<Runnable> tasks = threadPool.shutdownNow();
            if (!CollectionUtils.isEmpty(tasks)) {
                log.warn("[ProcessorTracker-{}] shutdown threadPool now and stop {} tasks.", instanceId, tasks.size());
            }
        });

        // 2. 去除顶层引用，送入GC世界
        taskTrackerActorRef = null;
        statusReportRetryQueue.clear();
        ProcessorTrackerPool.removeProcessorTracker(instanceId);

        log.info("[ProcessorTracker-{}] ProcessorTracker destroyed successfully!", instanceId);

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
        // 自定义线程池中线程名称 (PowerJob Processor Pool -> PPP)
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("PPP-%d").build();
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

        // PowerJob Processor TimingPool
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("PPT-%d").build();
        timingPool = Executors.newSingleThreadScheduledExecutor(threadFactory);

        timingPool.scheduleAtFixedRate(new CheckerAndReporter(), 0, 10, TimeUnit.SECONDS);
    }


    /**
     * 定时向 TaskTracker 汇报（携带任务执行信息的心跳）
     */
    private class CheckerAndReporter implements Runnable {

        @Override
        @SuppressWarnings({"squid:S1066","squid:S3776"})
        public void run() {

            // 超时检查，如果超时则自动关闭 TaskTracker
            long interval = System.currentTimeMillis() - startTime;
            // 秒级任务的ProcessorTracker不应该关闭
            if (!TimeExpressionType.FREQUENT_TYPES.contains(instanceInfo.getTimeExpressionType())) {
                if (interval > instanceInfo.getInstanceTimeoutMS()) {
                    log.warn("[ProcessorTracker-{}] detected instance timeout, maybe TaskTracker's destroy request missed, so try to kill self now.", instanceId);
                    destroy();
                    return;
                }
            }

            // 判断线程池活跃状态，长时间空闲则上报 TaskTracker 请求检查
            if (threadPool.getActiveCount() > 0 || threadPool.getCompletedTaskCount() > lastCompletedTaskCount) {
                lastIdleTime = -1;
                lastCompletedTaskCount = threadPool.getCompletedTaskCount();
            } else {
                if (lastIdleTime == -1) {
                    lastIdleTime = System.currentTimeMillis();
                } else {
                    long idleTime = System.currentTimeMillis() - lastIdleTime;
                    if (idleTime > MAX_IDLE_TIME) {
                        log.warn("[ProcessorTracker-{}] ProcessorTracker have been idle for {}ms, it's time to tell TaskTracker and then destroy self.", instanceId, idleTime);

                        // 不可靠通知，如果该请求失败，则整个任务处理集群缺失一个 ProcessorTracker，影响可接受
                        ProcessorTrackerStatusReportReq statusReportReq = ProcessorTrackerStatusReportReq.buildIdleReport(instanceId);
                        statusReportReq.setAddress(workerRuntime.getWorkerAddress());
                        taskTrackerActorRef.tell(statusReportReq, null);
                        destroy();
                        return;
                    }
                }
            }

            // 上报状态之前，先重新发送失败的任务，只要有结果堆积，就不上报状态（让 PT 认为该 TT 失联然后重试相关任务）
            while (!statusReportRetryQueue.isEmpty()) {
                ProcessorReportTaskStatusReq req = statusReportRetryQueue.poll();
                if (req != null) {
                    req.setReportTime(System.currentTimeMillis());
                    if (!AkkaUtils.reliableTransmit(taskTrackerActorRef, req)) {
                        statusReportRetryQueue.add(req);
                        log.warn("[ProcessorRunnable-{}] retry report finished task status failed: {}", instanceId, req);
                        return;
                    }
                }
            }

            // 上报当前 ProcessorTracker 负载
            long waitingNum = threadPool.getQueue().size();
            ProcessorTrackerStatusReportReq statusReportReq = ProcessorTrackerStatusReportReq.buildLoadReport(instanceId, waitingNum);
            statusReportReq.setAddress(workerRuntime.getWorkerAddress());
            taskTrackerActorRef.tell(statusReportReq, null);
            log.debug("[ProcessorTracker-{}] send heartbeat to TaskTracker, current waiting task num is {}.", instanceId, waitingNum);
        }

    }

    /**
     * 初始化处理器 Processor
     */
    private void initProcessor() throws Exception {

        ProcessorType processorType = ProcessorType.valueOf(instanceInfo.getProcessorType());
        String processorInfo = instanceInfo.getProcessorInfo();

        switch (processorType) {
            case BUILT_IN:
                // 先使用 Spring 加载
                if (SpringUtils.supportSpringBean()) {
                    try {
                        processor = SpringUtils.getBean(processorInfo);
                    } catch (Exception e) {
                        log.warn("[ProcessorTracker-{}] no spring bean of processor(className={}), reason is {}.", instanceId, processorInfo, ExceptionUtils.getMessage(e));
                    }
                }
                // 反射加载
                if (processor == null) {
                    processor = ProcessorBeanFactory.getInstance().getLocalProcessor(processorInfo);
                }
                break;
            case EXTERNAL:
                String[] split = processorInfo.split("#");
                log.info("[ProcessorTracker-{}] try to load processor({}) in container({})", instanceId, split[1], split[0]);

                String serverPath = AkkaUtils.getServerActorPath(workerRuntime.getServerDiscoveryService().getCurrentServerAddress());
                ActorSelection actorSelection = workerRuntime.getActorSystem().actorSelection(serverPath);
                omsContainer = OmsContainerFactory.fetchContainer(Long.valueOf(split[0]), actorSelection);
                if (omsContainer != null) {
                    processor = omsContainer.getProcessor(split[1]);
                } else {
                    log.warn("[ProcessorTracker-{}] load container failed.", instanceId);
                }
                break;
            default:
                log.warn("[ProcessorTracker-{}] unknown processor type: {}.", instanceId, processorType);
                throw new PowerJobException("unknown processor type of " + processorType);
        }

        if (processor == null) {
            log.warn("[ProcessorTracker-{}] fetch Processor(type={},info={}) failed.", instanceId, processorType, processorInfo);
            throw new PowerJobException("fetch Processor failed, please check your processorType and processorInfo config");
        }
    }

    /**
     * 计算线程池大小
     */
    private int calThreadPoolSize() {
        ExecuteType executeType = ExecuteType.valueOf(instanceInfo.getExecuteType());
        ProcessorType processorType = ProcessorType.valueOf(instanceInfo.getProcessorType());

        // 脚本类自带线程池，不过为了少一点逻辑判断，还是象征性分配一个线程
        if (processorType == ProcessorType.PYTHON || processorType == ProcessorType.SHELL) {
            return 1;
        }

        if (executeType == ExecuteType.MAP_REDUCE || executeType == ExecuteType.MAP) {
            return instanceInfo.getThreadConcurrency();
        }
        if (TimeExpressionType.FREQUENT_TYPES.contains(instanceInfo.getTimeExpressionType())) {
            return instanceInfo.getThreadConcurrency();
        }
        return 2;
    }

}
