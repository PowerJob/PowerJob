package tech.powerjob.server.common.timewheel;

import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.server.common.RejectedExecutionHandlerFactory;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 时间轮定时器
 * 支持的最小精度：1ms（Thread.sleep本身不精确导致精度没法提高）
 * 最小误差：1ms，理由同上
 *
 * @author tjq
 * @since 2020/4/2
 */
@Slf4j
public class HashedWheelTimer implements Timer {

    private final long tickDuration;
    private final HashedWheelBucket[] wheel;
    private final int mask;

    private final Indicator indicator;

    private final long startTime;

    private final Queue<HashedWheelTimerFuture> waitingTasks = Queues.newLinkedBlockingQueue();
    private final Queue<HashedWheelTimerFuture> canceledTasks = Queues.newLinkedBlockingQueue();

    private final ExecutorService taskProcessPool;

    public HashedWheelTimer(long tickDuration, int ticksPerWheel) {
        this(tickDuration, ticksPerWheel, 0);
    }

    /**
     * 新建时间轮定时器
     * @param tickDuration 时间间隔，单位毫秒（ms）
     * @param ticksPerWheel 轮盘个数
     * @param processThreadNum 处理任务的线程个数，0代表不启用新线程（如果定时任务需要耗时操作，请启用线程池）
     */
    public HashedWheelTimer(long tickDuration, int ticksPerWheel, int processThreadNum) {

        this.tickDuration = tickDuration;

        // 初始化轮盘，大小格式化为2的N次，可以使用 & 代替取余
        int ticksNum = CommonUtils.formatSize(ticksPerWheel);
        wheel = new HashedWheelBucket[ticksNum];
        for (int i = 0; i < ticksNum; i++) {
            wheel[i] = new HashedWheelBucket();
        }
        mask = wheel.length - 1;

        // 初始化执行线程池
        if (processThreadNum <= 0) {
            taskProcessPool = null;
        }else {
            ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("HashedWheelTimer-Executor-%d").build();
            // 这里需要调整一下队列大小
            BlockingQueue<Runnable> queue = Queues.newLinkedBlockingQueue(8192);
            int core = Math.max(Runtime.getRuntime().availableProcessors(), processThreadNum);
            // 基本都是 io 密集型任务
            taskProcessPool = new ThreadPoolExecutor(core, 2 * core,
                    60, TimeUnit.SECONDS,
                    queue, threadFactory, RejectedExecutionHandlerFactory.newCallerRun("PowerJobTimeWheelPool"));
        }

        startTime = System.currentTimeMillis();

        // 启动后台线程
        indicator = new Indicator();
        new Thread(indicator, "HashedWheelTimer-Indicator").start();
    }

    @Override
    public TimerFuture schedule(TimerTask task, long delay, TimeUnit unit) {

        long targetTime = System.currentTimeMillis() + unit.toMillis(delay);
        HashedWheelTimerFuture timerFuture = new HashedWheelTimerFuture(task, targetTime);

        // 直接运行到期、过期任务
        if (delay <= 0) {
            runTask(timerFuture);
            return timerFuture;
        }

        // 写入阻塞队列，保证并发安全（性能进一步优化可以考虑 Netty 的 Multi-Producer-Single-Consumer队列）
        waitingTasks.add(timerFuture);
        return timerFuture;
    }

    @Override
    public Set<TimerTask> stop() {
        indicator.stop.set(true);
        taskProcessPool.shutdown();
        while (!taskProcessPool.isTerminated()) {
            try {
                Thread.sleep(100);
            }catch (Exception ignore) {
            }
        }
        return indicator.getUnprocessedTasks();
    }

    /**
     * 包装 TimerTask，维护预期执行时间、总圈数等数据
     */
    private final class HashedWheelTimerFuture implements TimerFuture {

        // 预期执行时间
        private final long targetTime;
        private final TimerTask timerTask;

        // 所属的时间格，用于快速删除该任务
        private HashedWheelBucket bucket;
        // 总圈数
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
            return status == FINISHED;
        }
    }

    /**
     * 时间格（本质就是链表，维护了这个时刻可能需要执行的所有任务）
     */
    private final class HashedWheelBucket extends LinkedList<HashedWheelTimerFuture> {

        public void expireTimerTasks(long currentTick) {

            removeIf(timerFuture -> {

                // processCanceledTasks 后外部操作取消任务会导致 BUCKET 中仍存在 CANCELED 任务的情况
                if (timerFuture.status == HashedWheelTimerFuture.CANCELED) {
                    return true;
                }

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
                        // 提交执行
                        runTask(timerFuture);
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

    private void runTask(HashedWheelTimerFuture timerFuture) {
        timerFuture.status = HashedWheelTimerFuture.RUNNING;
        if (taskProcessPool == null) {
            timerFuture.timerTask.run();
        }else {
            taskProcessPool.submit(timerFuture.timerTask);
        }
    }

    /**
     * 模拟指针转动
     */
    private class Indicator implements Runnable {

        private long tick = 0;

        private final AtomicBoolean stop = new AtomicBoolean(false);
        private final CountDownLatch latch = new CountDownLatch(1);

        @Override
        public void run() {

            while (!stop.get()) {

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
            latch.countDown();
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
                HashedWheelBucket bucket = wheel[index];

                // TimerTask 维护 Bucket 引用，用于删除该任务
                timerTask.bucket = bucket;

                if (timerTask.status == HashedWheelTimerFuture.WAITING) {
                    bucket.add(timerTask);
                }
            }
        }

        public Set<TimerTask> getUnprocessedTasks() {
            try {
                latch.await();
            }catch (Exception ignore) {
            }

            Set<TimerTask> tasks = Sets.newHashSet();

            Consumer<HashedWheelTimerFuture> consumer = timerFuture -> {
                if (timerFuture.status == HashedWheelTimerFuture.WAITING) {
                    tasks.add(timerFuture.timerTask);
                }
            };

            waitingTasks.forEach(consumer);
            for (HashedWheelBucket bucket : wheel) {
                bucket.forEach(consumer);
            }
            return tasks;
        }
    }
}
