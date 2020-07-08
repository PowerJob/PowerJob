package com.github.kfcfans.powerjob.server.service.timing.schedule;

import com.github.kfcfans.powerjob.server.common.utils.timewheel.HashedWheelTimer;

/**
 * 时间轮单例
 *
 * @author tjq
 * @since 2020/4/5
 */
public class HashedWheelTimerHolder {

    // 精确时间轮，每 1S 走一格
    public static final HashedWheelTimer TIMER = new HashedWheelTimer(1, 4096, Runtime.getRuntime().availableProcessors() * 4);

    // 非精确时间轮，每 5S 走一格
    public static final HashedWheelTimer INACCURATE_TIMER = new HashedWheelTimer(5, 16, 0);

    private HashedWheelTimerHolder() {
    }
}
