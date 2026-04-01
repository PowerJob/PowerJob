package tech.powerjob.worker.core.executor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tech.powerjob.common.model.TaskGroupQuota;
import tech.powerjob.common.utils.SysUtils;
import tech.powerjob.worker.common.PowerJobWorkerConfig;
import tech.powerjob.worker.core.tracker.manager.LightTaskTrackerManager;

import java.util.Map;
import java.util.concurrent.*;

/**
 * @author Echo009
 * @since 2022/9/23
 */
@Slf4j
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
     * 执行轻量级任务 (default/fallback pool)
     */
    private final ExecutorService lightweightTaskExecutorService;

    /**
     * Per-group lightweight task executors. Null when task group isolation is not configured.
     */
    private final Map<String, ExecutorService> groupLightweightExecutors;


    public ExecutorManager(PowerJobWorkerConfig workerConfig) {

        final int availableProcessors = SysUtils.availableProcessors();
        // 初始化定时线程池
        ThreadFactory coreThreadFactory = new ThreadFactoryBuilder().setNameFormat("powerjob-worker-core-%d").build();
        coreExecutor = new ScheduledThreadPoolExecutor(3, coreThreadFactory);

        ThreadFactory lightTaskReportFactory = new ThreadFactoryBuilder().setNameFormat("powerjob-worker-light-task-status-check-%d").build();
        // 都是 io 密集型任务
        lightweightTaskStatusCheckExecutor = new ScheduledThreadPoolExecutor(availableProcessors * 10, lightTaskReportFactory);

        ThreadFactory lightTaskExecuteFactory = new ThreadFactoryBuilder().setNameFormat("powerjob-worker-light-task-execute-%d").build();
        // 大部分任务都是 io 密集型
        lightweightTaskExecutorService = new ThreadPoolExecutor(availableProcessors * 10, availableProcessors * 10, 120L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>((workerConfig.getMaxLightweightTaskNum() * 2), true), lightTaskExecuteFactory, new ThreadPoolExecutor.AbortPolicy());

        // Initialize per-group executors if configured
        Map<String, TaskGroupQuota> quotas = workerConfig.getTaskGroupQuotas();
        if (quotas != null && !quotas.isEmpty()) {
            validateTaskGroupQuotas(workerConfig);
            groupLightweightExecutors = new ConcurrentHashMap<>();
            int totalProcessors = availableProcessors * 10; // same as global pool core size
            int totalQuota = quotas.values().stream().mapToInt(TaskGroupQuota::getMaxLightweightTaskNum).sum();

            for (Map.Entry<String, TaskGroupQuota> entry : quotas.entrySet()) {
                String groupName = entry.getKey();
                TaskGroupQuota quota = entry.getValue();

                // Scale thread count proportionally to quota ratio
                int groupThreads = Math.max(1, (int) Math.round((double) quota.getMaxLightweightTaskNum() / totalQuota * totalProcessors));
                int queueSize = quota.getMaxLightweightTaskNum() * 2;

                ThreadFactory groupFactory = new ThreadFactoryBuilder()
                        .setNameFormat("powerjob-light-group-" + groupName + "-%d")
                        .build();

                ExecutorService groupExecutor = new ThreadPoolExecutor(
                        groupThreads, groupThreads, 120L, TimeUnit.SECONDS,
                        new ArrayBlockingQueue<>(queueSize, true),
                        groupFactory,
                        new ThreadPoolExecutor.AbortPolicy()
                );

                groupLightweightExecutors.put(groupName, groupExecutor);
                log.info("[ExecutorManager] created group executor: group={}, threads={}, queueSize={}, maxTasks={}",
                        groupName, groupThreads, queueSize, quota.getMaxLightweightTaskNum());
            }
        } else {
            groupLightweightExecutors = null;
        }
    }

    /**
     * Get the executor for a specific task group. Falls back to the default global pool
     * if groups are not configured or the group is unknown.
     */
    public ExecutorService getGroupLightweightTaskExecutorService(String groupName) {
        if (groupLightweightExecutors == null) {
            return lightweightTaskExecutorService;
        }
        ExecutorService executor = groupLightweightExecutors.get(groupName);
        if (executor != null) {
            return executor;
        }
        // Unknown group: fall back to "default" group, then to global pool
        executor = groupLightweightExecutors.get(TaskGroupQuota.DEFAULT_GROUP);
        if (executor != null) {
            log.warn("[ExecutorManager] unknown taskGroup '{}', falling back to 'default' group", groupName);
            return executor;
        }
        return lightweightTaskExecutorService;
    }

    /**
     * Validate task group quota configuration at startup.
     */
    private void validateTaskGroupQuotas(PowerJobWorkerConfig config) {
        Map<String, TaskGroupQuota> quotas = config.getTaskGroupQuotas();

        if (!quotas.containsKey(TaskGroupQuota.DEFAULT_GROUP)) {
            throw new IllegalArgumentException(
                    "[ExecutorManager] taskGroupQuotas must contain a 'default' group. " +
                    "The 'default' group handles all jobs without an explicit taskGroup and jobs with unknown groups.");
        }

        int totalQuota = 0;
        for (Map.Entry<String, TaskGroupQuota> entry : quotas.entrySet()) {
            if (entry.getValue().getMaxLightweightTaskNum() <= 0) {
                throw new IllegalArgumentException(
                        "[ExecutorManager] group '" + entry.getKey() + "' maxLightweightTaskNum must be > 0");
            }
            totalQuota += entry.getValue().getMaxLightweightTaskNum();
        }

        if (totalQuota > config.getMaxLightweightTaskNum()) {
            throw new IllegalArgumentException(
                    "[ExecutorManager] sum of group quotas (" + totalQuota +
                    ") exceeds maxLightweightTaskNum (" + config.getMaxLightweightTaskNum() +
                    "). Reduce group quotas or increase maxLightweightTaskNum.");
        }

        if (totalQuota < config.getMaxLightweightTaskNum()) {
            log.warn("[ExecutorManager] sum of group quotas ({}) is less than maxLightweightTaskNum ({}). {} task slots are unallocated.",
                    totalQuota, config.getMaxLightweightTaskNum(), config.getMaxLightweightTaskNum() - totalQuota);
        }
    }


    public void shutdown() {
        coreExecutor.shutdownNow();
        lightweightTaskExecutorService.shutdownNow();
        lightweightTaskStatusCheckExecutor.shutdownNow();

        // Shut down per-group executors
        if (groupLightweightExecutors != null) {
            groupLightweightExecutors.forEach((group, executor) -> {
                log.info("[ExecutorManager] shutting down group executor: {}", group);
                executor.shutdownNow();
            });
        }

        // Clear static tracking maps
        LightTaskTrackerManager.clearAll();
    }

}
