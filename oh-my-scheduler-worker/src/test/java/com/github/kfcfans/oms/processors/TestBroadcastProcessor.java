package com.github.kfcfans.oms.processors;

import com.github.kfcfans.common.utils.JsonUtils;
import com.github.kfcfans.oms.worker.core.processor.ProcessResult;
import com.github.kfcfans.oms.worker.core.processor.TaskContext;
import com.github.kfcfans.oms.worker.core.processor.TaskResult;
import com.github.kfcfans.oms.worker.core.processor.sdk.BroadcastProcessor;

import java.util.List;
import java.util.Map;

/**
 * 测试用的广播执行处理器
 *
 * @author tjq
 * @since 2020/3/25
 */
public class TestBroadcastProcessor extends BroadcastProcessor {
    @Override
    public ProcessResult preProcess(TaskContext taskContext) throws Exception {
        System.out.println("=============== TestBroadcastProcessor#preProcess ===============");
        System.out.println("taskContext:" + JsonUtils.toJSONString(taskContext));
        return new ProcessResult(true, "preProcess success");
    }

    @Override
    public ProcessResult postProcess(TaskContext taskContext, List<TaskResult> taskResults) throws Exception {
        System.out.println("=============== TestBroadcastProcessor#postProcess ===============");
        System.out.println("taskContext:" + JsonUtils.toJSONString(taskContext));
        System.out.println("taskId2Result:" + taskResults);
        return new ProcessResult(true, "postProcess success");
    }

    @Override
    public ProcessResult process(TaskContext context) throws Exception {
        System.out.println("=============== TestBroadcastProcessor#process ===============");
        System.out.println("taskContext:" + JsonUtils.toJSONString(context));
        return new ProcessResult(true, "processSuccess");
    }
}
