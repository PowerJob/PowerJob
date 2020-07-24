package com.github.kfcfans.powerjob.server.service.instance;

import com.github.kfcfans.powerjob.server.common.utils.timewheel.HashedWheelTimer;
import com.github.kfcfans.powerjob.server.common.utils.timewheel.TimerFuture;
import com.github.kfcfans.powerjob.server.common.utils.timewheel.TimerTask;
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

    // 精确时间轮，每 1S 走一格
    private static final HashedWheelTimer TIMER = new HashedWheelTimer(1, 4096, Runtime.getRuntime().availableProcessors() * 4);

    /**
     * 定时调度
     * @param uniqueId 唯一 ID，必须是 snowflake 算法生成的 ID
     * @param delayMS 延迟毫秒数
     * @param timerTask 需要执行的目标方法
     */
    public static void schedule(Long uniqueId, Long delayMS, TimerTask timerTask) {
        TimerFuture timerFuture = TIMER.schedule(() -> {
            CARGO.remove(uniqueId);
            timerTask.run();
        }, delayMS, TimeUnit.MILLISECONDS);

        CARGO.put(uniqueId, timerFuture);
    }

    /**
     * 获取 TimerFuture
     * @param uniqueId 唯一 ID
     * @return TimerFuture
     */
    public static TimerFuture fetchTimerFuture(Long uniqueId) {
        return CARGO.get(uniqueId);
    }

}
