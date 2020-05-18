package com.github.kfcfans.oms.worker.container;

import com.github.kfcfans.oms.worker.core.processor.sdk.BasicProcessor;

/**
 * OhMyScheduler 容器规范
 *
 * @author tjq
 * @since 2020/5/15
 */
public interface OmsContainer extends LifeCycle {

    /**
     * 获取处理器
     * @param className 全限定类名
     * @return 处理器（可以是 MR、BD等处理器）
     */
    BasicProcessor getProcessor(String className);

    String getName();
    String getVersion();

    /**
     * 尝试释放容器资源
     */
    void tryRelease();
}
