package com.github.kfcfans.oms.worker.core.processor.sdk;

import com.github.kfcfans.oms.worker.core.processor.TaskContext;
import com.github.kfcfans.oms.worker.core.processor.ProcessResult;

/**
 * 基础的处理器，适用于单机执行
 *
 * @author tjq
 * @since 2020/3/18
 */
public interface BasicProcessor {

    /**
     * 核心处理逻辑
     * @param context 任务上下文，可通过 jobParams 和 instanceParams 分别获取控制台参数和OpenAPI传递的任务实例参数
     * @return 处理结果，msg有长度限制，超长会被裁剪，不允许返回 null
     * @throws Exception 异常，允许抛出异常，但不推荐，最好由业务开发者自己处理
     */
    ProcessResult process(TaskContext context) throws Exception;

    /**
     * 用于构造 Processor 对象，相当于构造方法
     * @throws Exception 异常，抛出异常则视为处理器构造失败，任务直接失败
     */
    default void init() throws Exception {
    }

    /**
     * 销毁 Processor 时的回调方法，暂时未被使用
     * @throws Exception 异常
     */
    default void destroy() throws Exception {
    }

}
