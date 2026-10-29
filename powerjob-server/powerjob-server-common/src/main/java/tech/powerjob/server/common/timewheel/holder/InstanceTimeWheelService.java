package tech.powerjob.server.common.timewheel.holder;

import tech.powerjob.common.utils.SysUtils;
import tech.powerjob.server.common.timewheel.HashedWheelTimer;
import tech.powerjob.server.common.timewheel.Timer;
import tech.powerjob.server.common.timewheel.TimerFuture;
import tech.powerjob.server.common.timewheel.TimerTask;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 定时调度任务实例
 *
 * @author tjq
 * @since 2020/7/25
 */
public class InstanceTimeWheelService {

    private static final Map<Long, TimerFuture> CARGO = Maps.newConcurrentMap();

    /**
     * 精确调度时间轮，每 1MS 走一格
     */
    private static final Timer TIMER = new HashedWheelTimer(1, 4096, SysUtils.availableProcessors() * 4);
    /**
     * 非精确调度时间轮，用于处理高延迟任务，每 10S 走一格
     */
    private static final Timer SLOW_TIMER = new HashedWheelTimer(10000, 12, 0);

    /**
     * 支持取消的时间间隔，低于该阈值则不会放进 CARGO
     */
    private static final long MIN_INTERVAL_MS = 1000;
    /**
     * 长延迟阈值
     */
    private static final long LONG_DELAY_THRESHOLD_MS = 60000;

    /**
     * 定时调度
     * @param uniqueId 唯一 ID，必须是 snowflake 算法生成的 ID
     * @param delayMS 延迟毫秒数
     * @param timerTask 需要执行的目标方法
     */
    public static void schedule(Long uniqueId, Long delayMS, TimerTask timerTask) {
        if (delayMS <= LONG_DELAY_THRESHOLD_MS) {
            realSchedule(uniqueId, delayMS, timerTask);
            return;
        }

        long expectTriggerTime = System.currentTimeMillis() + delayMS;
        TimerFuture longDelayTask = SLOW_TIMER.schedule(() -> {
            CARGO.remove(uniqueId);
            realSchedule(uniqueId, expectTriggerTime - System.currentTimeMillis(), timerTask);
        }, delayMS - LONG_DELAY_THRESHOLD_MS, TimeUnit.MILLISECONDS);
        CARGO.put(uniqueId, new CargoTimerFuture(uniqueId, longDelayTask));
    }

    /**
     * 获取 TimerFuture
     * @param uniqueId 唯一 ID
     * @return TimerFuture
     */
    public static TimerFuture fetchTimerFuture(Long uniqueId) {
        return CARGO.get(uniqueId);
    }


    private static void realSchedule(Long uniqueId, Long delayMS, TimerTask timerTask) {
        TimerFuture timerFuture = TIMER.schedule(() -> {
            CARGO.remove(uniqueId);
            timerTask.run();
        }, delayMS, TimeUnit.MILLISECONDS);
        if (delayMS > MIN_INTERVAL_MS) {
            CARGO.put(uniqueId, new CargoTimerFuture(uniqueId, timerFuture));
        }
    }


    /**
     * 包装 TimerFuture，取消成功后移除 CARGO 中的引用
     */
    private static final class CargoTimerFuture implements TimerFuture {

        private final Long uniqueId;
        private final TimerFuture delegate;

        private CargoTimerFuture(Long uniqueId, TimerFuture delegate) {
            this.uniqueId = uniqueId;
            this.delegate = delegate;
        }

        @Override
        public boolean cancel() {
            if (!delegate.cancel()) {
                return false;
            }
            CARGO.remove(uniqueId, this);
            return true;
        }

        @Override
        public TimerTask getTask() {
            return delegate.getTask();
        }

        @Override
        public boolean isCancelled() {
            return delegate.isCancelled();
        }

        @Override
        public boolean isDone() {
            return delegate.isDone();
        }
    }

}
