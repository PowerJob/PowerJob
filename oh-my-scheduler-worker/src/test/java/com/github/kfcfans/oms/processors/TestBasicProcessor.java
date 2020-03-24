package com.github.kfcfans.oms.processors;

import com.github.kfcfans.oms.worker.sdk.ProcessResult;
import com.github.kfcfans.oms.worker.sdk.TaskContext;
import com.github.kfcfans.oms.worker.sdk.api.BasicProcessor;

/**
 * 测试用的基础处理器
 *
 * @author tjq
 * @since 2020/3/24
 */
public class TestBasicProcessor implements BasicProcessor {

    @Override
    public ProcessResult process(TaskContext context) throws Exception {
        System.out.println("==== ProcessResult#process");
        System.out.println("TaskContext: " + context.toString());
        return new ProcessResult(true, "success");
    }
}
