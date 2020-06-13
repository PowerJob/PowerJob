package com.github.kfcfans.powerjob.server.service.timing.schedule;

import com.github.kfcfans.powerjob.server.common.utils.timewheel.HashedWheelTimer;

/**
 * 时间轮单例
 *
 * @author tjq
 * @since 2020/4/5
 */
public class HashedWheelTimerHolder {

    public static final HashedWheelTimer TIMER = new HashedWheelTimer(1, 4096, Runtime.getRuntime().availableProcessors() * 4);

    private HashedWheelTimerHolder() {
    }
}
