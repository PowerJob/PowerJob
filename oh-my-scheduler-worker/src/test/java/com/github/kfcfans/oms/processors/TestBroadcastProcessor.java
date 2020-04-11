package com.github.kfcfans.oms.processors;

import com.alibaba.fastjson.JSONObject;
import com.github.kfcfans.oms.worker.sdk.ProcessResult;
import com.github.kfcfans.oms.worker.sdk.TaskContext;
import com.github.kfcfans.oms.worker.sdk.api.BroadcastProcessor;

import java.util.Map;

/**
 * 测试用的广播执行处理器
 *
 * @author tjq
 * @since 2020/3/25
 */
public class TestBroadcastProcessor implements BroadcastProcessor {
    @Override
    public ProcessResult preProcess(TaskContext taskContext) throws Exception {
        System.out.println("=============== TestBroadcastProcessor#preProcess ===============");
        System.out.println("taskContext:" + JSONObject.toJSONString(taskContext));
        return new ProcessResult(true, "preProcess success");
    }

    @Override
    public ProcessResult postProcess(TaskContext taskContext, Map<String, String> taskId2Result) throws Exception {
        System.out.println("=============== TestBroadcastProcessor#postProcess ===============");
        System.out.println("taskContext:" + JSONObject.toJSONString(taskContext));
        System.out.println("taskId2Result:" + taskId2Result);
        return new ProcessResult(true, "postProcess success");
    }

    @Override
    public ProcessResult process(TaskContext context) throws Exception {
        System.out.println("=============== TestBroadcastProcessor#process ===============");
        System.out.println("taskContext:" + JSONObject.toJSONString(context));
        return new ProcessResult(true, "processSuccess");
    }
}
