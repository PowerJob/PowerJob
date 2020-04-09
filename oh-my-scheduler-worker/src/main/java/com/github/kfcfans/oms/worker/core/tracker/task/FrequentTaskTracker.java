package com.github.kfcfans.oms.worker.core.tracker.task;

import com.github.kfcfans.common.ExecuteType;
import com.github.kfcfans.common.InstanceStatus;
import com.github.kfcfans.common.ProcessorType;
import com.github.kfcfans.common.TimeExpressionType;
import com.github.kfcfans.common.request.ServerScheduleJobReq;
import com.github.kfcfans.oms.worker.OhMyWorker;
import com.github.kfcfans.oms.worker.common.constants.TaskConstant;
import com.github.kfcfans.oms.worker.common.constants.TaskStatus;
import com.github.kfcfans.oms.worker.common.utils.LRUCache;
import com.github.kfcfans.oms.worker.persistence.TaskDO;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 处理秒级任务（FIX_RATE/FIX_DELAY）的TaskTracker
 *
 * @author tjq
 * @since 2020/4/8
 */
@Slf4j
public class FrequentTaskTracker extends TaskTracker {

    // 总运行次数（正常情况不会出现锁竞争，直接用 Atomic 系列，锁竞争验证推荐 LongAdder）
    private final AtomicLong triggerTimes = new AtomicLong(0);
    // 保存最近10个子任务的信息，供用户查询（user -> server -> worker 传递查询）
    private final LRUCache<Long, SubInstanceInfo> recentSubInstanceInfo = new LRUCache<>(HISTORY_SIZE);
    // 保存运行中的任务
    private final Map<Long, Long> subInstanceId2LastActiveTime = Maps.newConcurrentMap();

    private static final int HISTORY_SIZE = 10;

    public FrequentTaskTracker(ServerScheduleJobReq req) {
        super(req);
    }

    @Override
    protected void initTaskTracker(ServerScheduleJobReq req) {

        // 1. 初始化定时调度线程池
        ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("oms-TaskTrackerTimingPool-%d").build();
        this.scheduledPool = Executors.newScheduledThreadPool(3, factory);

        // 2. 启动任务发射器
        Runnable launcher = new Launcher();
        long t = Long.parseLong(req.getTimeExpression());
        TimeExpressionType timeExpressionType = TimeExpressionType.valueOf(req.getTimeExpressionType());
        if (timeExpressionType == TimeExpressionType.FIX_RATE) {
            scheduledPool.scheduleAtFixedRate(launcher, 0, t, TimeUnit.SECONDS);
        }else {
            scheduledPool.scheduleWithFixedDelay(launcher, 0, t, TimeUnit.SECONDS);
        }

        // 3. 启动任务分发器（事实上，秒级任务应该都是单机任务，且感觉不需要失败重试机制，那么 Dispatcher 的存在就有点浪费系统资源了...）
        scheduledPool.scheduleWithFixedDelay(new Dispatcher(), 1, 2, TimeUnit.SECONDS);

    }


    @Override
    public void updateTaskStatus(String taskId, int newStatus, @Nullable String result) {

        super.updateTaskStatus(taskId, newStatus, result);
    }

    /**
     * 任务发射器（@Reference 饥荒->雪球发射器）
     */
    private class Launcher implements Runnable {

        @Override
        public void run() {

            Long subInstanceId = triggerTimes.incrementAndGet();
            subInstanceId2LastActiveTime.put(subInstanceId, System.currentTimeMillis());

            String myAddress = OhMyWorker.getWorkerAddress();
            String taskId = String.valueOf(subInstanceId);

            TaskDO newRootTask = new TaskDO();

            newRootTask.setSubInstanceId(subInstanceId);
            newRootTask.setInstanceId(instanceInfo.getInstanceId());
            newRootTask.setTaskId(taskId);

            newRootTask.setStatus(TaskStatus.DISPATCH_SUCCESS_WORKER_UNCHECK.getValue());
            newRootTask.setFailedCnt(0);
            // 根任务总是默认本机执行
            newRootTask.setAddress(myAddress);
            newRootTask.setTaskName(TaskConstant.ROOT_TASK_NAME);
            newRootTask.setCreatedTime(System.currentTimeMillis());
            newRootTask.setLastModifiedTime(System.currentTimeMillis());

            // 秒级任务要求精确，先运行再说～
            dispatchTask(newRootTask, myAddress);


            // 持久化
            if (!taskPersistenceService.save(newRootTask)) {
                log.error("[TaskTracker-{}] Launcher create new root task failed.", instanceId);
            }else {
                log.debug("[TaskTracker-{}] Launcher create new root task successfully.", instanceId);
            }
        }
    }

    private class Checker implements Runnable {

        @Override
        public void run() {
            Stopwatch stopwatch = Stopwatch.createStarted();
            ExecuteType executeType = ExecuteType.valueOf(instanceInfo.getExecuteType());
            long instanceTimeoutMS = instanceInfo.getInstanceTimeoutMS();
            long nowTS = System.currentTimeMillis();

            subInstanceId2LastActiveTime.forEach((subInstanceId, lastActiveTime) -> {

                long timeout = nowTS - lastActiveTime;

                // 超时，直接判定为失败
                if (timeout > instanceTimeoutMS) {

                    // 更新缓存数据
                    if (recentSubInstanceInfo.containsKey(subInstanceId)) {
                        SubInstanceInfo subInstanceInfo = recentSubInstanceInfo.get(subInstanceId);
                        subInstanceInfo.status = InstanceStatus.FAILED.getV();
                        subInstanceInfo.result = "TIMEOUT";
                    }

                    // 删除数据库相关数据
                }

            });
        }
    }

    private static class SubInstanceInfo {
        private int status;
        private String result;
    }

}
