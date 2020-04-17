package com.github.kfcfans.oms.server.processors;

import com.alibaba.fastjson.JSONObject;
import com.github.kfcfans.oms.worker.core.processor.ProcessResult;
import com.github.kfcfans.oms.worker.core.processor.TaskContext;
import com.github.kfcfans.oms.worker.core.processor.TaskResult;
import com.github.kfcfans.oms.worker.core.processor.sdk.BroadcastProcessor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 广播处理器 示例
 * com.github.kfcfans.oms.server.processors.BroadcastProcessorDemo
 *
 * @author tjq
 * @since 2020/4/17
 */
@Slf4j
public class BroadcastProcessorDemo extends BroadcastProcessor {

    @Override
    public ProcessResult preProcess(TaskContext context) throws Exception {

        System.out.println("================ BroadcastProcessorDemo#preProcess ================");
        System.out.println("TaskContext: " + JSONObject.toJSONString(context));

        boolean success = ThreadLocalRandom.current().nextBoolean();
        return new ProcessResult(success, context + ": " + success);
    }

    @Override
    public ProcessResult process(TaskContext context) throws Exception {
        System.out.println("================ BroadcastProcessorDemo#process ================");
        System.out.println("TaskContext: " + JSONObject.toJSONString(context));

        boolean success = ThreadLocalRandom.current().nextBoolean();
        return new ProcessResult(success, context + ": " + success);
    }

    @Override
    public ProcessResult postProcess(TaskContext context, List<TaskResult> taskResults) throws Exception {

        System.out.println("================ BroadcastProcessorDemo#postProcess ================");
        System.out.println("TaskContext: " + JSONObject.toJSONString(context));
        System.out.println("List<TaskResult>: " + JSONObject.toJSONString(taskResults));

        boolean success = ThreadLocalRandom.current().nextBoolean();
        return new ProcessResult(success, context + ": " + success);
    }
}
