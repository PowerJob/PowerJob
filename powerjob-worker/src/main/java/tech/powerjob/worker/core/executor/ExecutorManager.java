package tech.powerjob.worker.core.executor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Getter;
import tech.powerjob.worker.common.PowerJobWorkerConfig;

import java.util.concurrent.*;

/**
 * @author Echo009
 * @since 2022/9/23
 */
@Getter
public class ExecutorManager {
    /**
     * 执行 Worker 底层核心任务
     */
    private final ScheduledExecutorService coreExecutor;
    /**
     * 执行轻量级任务状态上报
     */
    private final ScheduledExecutorService lightweightTaskStatusCheckExecutor;
    /**
     * 执行轻量级任务
     */
    private final ExecutorService lightweightTaskExecutorService;


    public ExecutorManager(PowerJobWorkerConfig workerConfig){


        final int availableProcessors = Runtime.getRuntime().availableProcessors();
        // 初始化定时线程池
        ThreadFactory coreThreadFactory = new ThreadFactoryBuilder().setNameFormat("powerjob-worker-core-%d").build();
        coreExecutor =  new ScheduledThreadPoolExecutor(3, coreThreadFactory);

        ThreadFactory lightTaskReportFactory = new ThreadFactoryBuilder().setNameFormat("powerjob-worker-light-task-status-check-%d").build();
        // 都是 io 密集型任务
        lightweightTaskStatusCheckExecutor =  new ScheduledThreadPoolExecutor(availableProcessors * 10, lightTaskReportFactory);

        ThreadFactory lightTaskExecuteFactory = new ThreadFactoryBuilder().setNameFormat("powerjob-worker-light-task-execute-%d").build();
        // 大部分任务都是 io 密集型
        lightweightTaskExecutorService = new ThreadPoolExecutor(availableProcessors * 10,availableProcessors * 10, 120L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>((workerConfig.getMaxLightweightTaskNum() * 2),true), lightTaskExecuteFactory, new ThreadPoolExecutor.AbortPolicy());

    }



    public void shutdown(){
        coreExecutor.shutdownNow();
    }

}
