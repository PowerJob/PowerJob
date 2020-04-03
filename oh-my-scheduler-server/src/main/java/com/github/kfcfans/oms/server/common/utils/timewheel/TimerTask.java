package com.github.kfcfans.oms.server.common.utils.timewheel;

/**
 * 时间任务接口
 *
 * @author tjq
 * @since 2020/4/2
 */
public interface TimerTask {

    /**
     * 正常执行时调用
     */
    void onScheduled();

    /**
     * 被取消时调用
     */
    default void onCanceled() {
    }
}
