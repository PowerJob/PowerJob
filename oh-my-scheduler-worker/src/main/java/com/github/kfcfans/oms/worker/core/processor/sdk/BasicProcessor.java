package com.github.kfcfans.oms.worker.core.processor.sdk;

import com.github.kfcfans.oms.worker.core.processor.TaskContext;
import com.github.kfcfans.oms.worker.core.processor.ProcessResult;

/**
 * 基础的处理器，适用于单机执行
 * TODO：真实API不包含异常抛出，为了便于开发先加上
 *
 * @author tjq
 * @since 2020/3/18
 */
public interface BasicProcessor {

    ProcessResult process(TaskContext context) throws Exception;

    /**
     * Processor 初始化方法
     */
    default void init() throws Exception {
    }

    /**
     * Processor 销毁方法
     */
    default void destroy() throws Exception {
    }

}
