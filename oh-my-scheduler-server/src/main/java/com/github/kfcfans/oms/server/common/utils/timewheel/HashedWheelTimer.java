package com.github.kfcfans.oms.server.common.utils.timewheel;

import com.google.common.collect.Queues;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 * 时间轮定时器
 *
 * @author tjq
 * @since 2020/4/2
 */
@Slf4j
public class HashedWheelTimer implements Timer {

    private final long tickDuration;
    private final HashedWheelBucket[] wheel;
    private final int mask;

    private final Thread indicatorThread;

    private long startTime;

    private final Queue<HashedWheelTimerFuture> waitingTasks = Queues.newLinkedBlockingQueue();
    private final Queue<HashedWheelTimerFuture> canceledTasks = Queues.newLinkedBlockingQueue();

    private static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * 新建时间轮定时器
     * @param tickDuration 时间间隔，单位毫秒（ms）
     * @param ticksPerWheel 轮盘个数
     */
    public HashedWheelTimer(long tickDuration, int ticksPerWheel) {

        this.tickDuration = tickDuration;

        // 初始化轮盘，大小格式化为2的N次，可以使用 & 代替取余
        int ticksNum = formatSize(ticksPerWheel);
        wheel = new HashedWheelBucket[ticksNum];
        for (int i = 0; i < ticksNum; i++) {
            wheel[i] = new HashedWheelBucket();
        }
        mask = wheel.length - 1;

        startTime = System.currentTimeMillis();

        // 启动后台线程
        indicatorThread = new Thread(new IndicatorRunnable(), "HashedWheelTimer-Indicator");
        indicatorThread.start();
    }

    @Override
    public TimerFuture schedule(TimerTask task, long delay, TimeUnit unit) {

        long targetTime = System.currentTimeMillis() + unit.toMillis(delay);
        HashedWheelTimerFuture timerFuture = new HashedWheelTimerFuture(task, targetTime);

        // 写入阻塞队列，保证并发安全（性能进一步优化可以考虑 Netty 的 Multi-Producer-Single-Consumer队列）
        waitingTasks.add(timerFuture);

        return timerFuture;
    }

    @Override
    public void stop() {

    }

    private final class HashedWheelTimerFuture implements TimerFuture {

        // 预期执行时间
        private final long targetTime;
        private final TimerTask timerTask;

        // 所属的时间格，用于快速删除该任务
        private HashedWheelBucket bucket;
        // 剩余圈数
        private long totalTicks;
        // 当前状态 0 - 初始化等待中，1 - 运行中，2 - 完成，3 - 已取消
        private int status;

        // 状态枚举值
        private static final int WAITING = 0;
        private static final int RUNNING = 1;
        private static final int FINISHED = 2;
        private static final int CANCELED = 3;

        public HashedWheelTimerFuture(TimerTask timerTask, long targetTime) {

            this.targetTime = targetTime;
            this.timerTask = timerTask;
            this.status = WAITING;
        }

        @Override
        public TimerTask getTask() {
            return timerTask;
        }

        @Override
        public boolean cancel() {
            if (status == WAITING) {
                status = CANCELED;
                canceledTasks.add(this);
                return true;
            }
            return false;
        }

        @Override
        public boolean isCancelled() {
            return status == CANCELED;
        }

        @Override
        public boolean isDone() {
            return startTime == FINISHED;
        }
    }

    private static final class HashedWheelBucket extends LinkedList<HashedWheelTimerFuture> {

        public void expireTimerTasks(long currentTick) {

            removeIf(timerFuture -> {

                if (timerFuture.status != HashedWheelTimerFuture.WAITING) {
                    log.warn("[HashedWheelTimer] impossible, please fix the bug");
                    return true;
                }

                // 本轮直接调度
                if (timerFuture.totalTicks <= currentTick) {

                    if (timerFuture.totalTicks < currentTick) {
                        log.warn("[HashedWheelTimer] timerFuture.totalTicks < currentTick, please fix the bug");
                    }

                    try {
                        timerFuture.timerTask.onScheduled();
                    }catch (Exception ignore) {

                    } finally {
                        timerFuture.status = HashedWheelTimerFuture.FINISHED;
                    }
                    return true;
                }

                return false;
            });

        }

    }

    /**
     * 模拟时钟转动
     */
    private class IndicatorRunnable implements Runnable {

        private long tick = 0;

        @Override
        public void run() {
            while (true) {

                // 1. 将任务从队列推入时间轮
                pushTaskToBucket();
                // 2. 处理取消的任务
                processCanceledTasks();
                // 3. 等待指针跳向下一刻
                tickTack();
                // 4. 执行定时任务
                int currentIndex = (int) (tick & mask);
                HashedWheelBucket bucket = wheel[currentIndex];
                bucket.expireTimerTasks(tick);

                tick ++;
            }
        }

        /**
         * 模拟指针转动，当返回时指针已经转到了下一个刻度
         */
        private void tickTack() {

            // 下一次调度的绝对时间
            long nextTime = startTime + (tick + 1) * tickDuration;
            long sleepTime = nextTime - System.currentTimeMillis();

            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                }catch (Exception ignore) {
                }
            }
        }

        /**
         * 处理被取消的任务
         */
        private void processCanceledTasks() {
            while (true) {
                HashedWheelTimerFuture canceledTask = canceledTasks.poll();
                if (canceledTask == null) {
                    return;
                }
                // 从链表中删除该任务（bucket为null说明还没被正式推入时间格中，不需要处理）
                if (canceledTask.bucket != null) {
                    canceledTask.bucket.remove(canceledTask);
                }
                // 调用回调方法
                try {
                    canceledTask.timerTask.onCanceled();
                }catch (Exception ignore) {
                }
            }
        }

        /**
         * 将队列中的任务推入时间轮中
         */
        private void pushTaskToBucket() {

            while (true) {
                HashedWheelTimerFuture timerTask = waitingTasks.poll();
                if (timerTask == null) {
                    return;
                }

                // 总共的偏移量
                long offset = timerTask.targetTime - startTime;
                // 总共需要走的指针步数
                timerTask.totalTicks = offset / tickDuration;
                // 取余计算 bucket index
                int index = (int) (timerTask.totalTicks & mask);

                if (timerTask.status == HashedWheelTimerFuture.WAITING) {
                    wheel[index].add(timerTask);
                }
            }
        }
    }

    private static int formatSize(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }
}
