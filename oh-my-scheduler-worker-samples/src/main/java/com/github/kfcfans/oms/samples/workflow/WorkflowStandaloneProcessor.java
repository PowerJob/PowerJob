package com.github.kfcfans.oms.samples.workflow;

import com.alibaba.fastjson.JSONObject;
import com.github.kfcfans.oms.worker.core.processor.ProcessResult;
import com.github.kfcfans.oms.worker.core.processor.TaskContext;
import com.github.kfcfans.oms.worker.core.processor.sdk.BasicProcessor;
import com.github.kfcfans.oms.worker.log.OmsLogger;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 工作流测试
 *
 * @author tjq
 * @since 2020/6/2
 */
@Component
public class WorkflowStandaloneProcessor implements BasicProcessor {

    @Override
    public ProcessResult process(TaskContext context) throws Exception {
        OmsLogger logger = context.getOmsLogger();
        logger.info("current:" + context.getJobParams());
        System.out.println("current: " + context.getJobParams());
        System.out.println("currentContext:");
        System.out.println(JSONObject.toJSONString(context));

        // 尝试获取上游任务
        Map<Long, String> upstreamTaskResult = context.fetchUpstreamTaskResult();
        System.out.println("工作流上游任务数据：");
        System.out.println(upstreamTaskResult);

        return new ProcessResult(true, context.getJobId() + " process successfully.");
    }
}
