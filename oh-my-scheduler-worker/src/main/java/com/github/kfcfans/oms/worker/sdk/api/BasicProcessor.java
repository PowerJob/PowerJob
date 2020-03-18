package com.github.kfcfans.oms.worker.sdk.api;

import com.github.kfcfans.oms.worker.sdk.TaskContext;
import com.github.kfcfans.oms.worker.sdk.ProcessResult;

/**
 * 基础的处理器，适用于单机执行
 *
 * @author tjq
 * @since 2020/3/18
 */
public interface BasicProcessor {

    ProcessResult process(TaskContext context);

}
