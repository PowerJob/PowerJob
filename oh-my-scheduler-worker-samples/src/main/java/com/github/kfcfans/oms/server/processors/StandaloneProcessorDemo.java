package com.github.kfcfans.oms.server.processors;

import com.alibaba.fastjson.JSONObject;
import com.github.kfcfans.oms.worker.core.processor.ProcessResult;
import com.github.kfcfans.oms.worker.core.processor.TaskContext;
import com.github.kfcfans.oms.worker.core.processor.sdk.BasicProcessor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 单机处理器 示例
 * com.github.kfcfans.oms.server.processors.StandaloneProcessorDemo
 *
 * @author tjq
 * @since 2020/4/17
 */
@Slf4j
public class StandaloneProcessorDemo implements BasicProcessor {

    @Override
    public ProcessResult process(TaskContext context) throws Exception {

        System.out.println("================ StandaloneProcessorDemo#process ================");
        System.out.println("TaskContext: " + JSONObject.toJSONString(context));

        boolean success = ThreadLocalRandom.current().nextBoolean();
        return new ProcessResult(success, context + ": " + success);
    }
}
