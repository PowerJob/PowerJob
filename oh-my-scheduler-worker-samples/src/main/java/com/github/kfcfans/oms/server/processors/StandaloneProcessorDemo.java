package com.github.kfcfans.oms.server.processors;

import com.alibaba.fastjson.JSONObject;
import com.github.kfcfans.oms.worker.core.processor.ProcessResult;
import com.github.kfcfans.oms.worker.core.processor.TaskContext;
import com.github.kfcfans.oms.worker.core.processor.sdk.BasicProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 单机处理器 示例
 * com.github.kfcfans.oms.server.processors.StandaloneProcessorDemo
 *
 * @author tjq
 * @since 2020/4/17
 */
@Slf4j
@Component
public class StandaloneProcessorDemo implements BasicProcessor {

    @Override
    public ProcessResult process(TaskContext context) throws Exception {

        context.getOmsLogger().info("StandaloneProcessorDemo start process,context is {}.", context);
        System.out.println("================ StandaloneProcessorDemo#process ================");
        // 根据控制台参数判断是否成功
        boolean success = "success".equals(context.getJobParams());
        System.out.println("TaskContext: " + JSONObject.toJSONString(context));
        System.out.println("ProcessSuccess: " + success);
        context.getOmsLogger().info("StandaloneProcessorDemo finished process,success: .", success);
        return new ProcessResult(success, context + ": " + success);
    }
}
