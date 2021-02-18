package com.github.kfcfans.powerjob.samples.tester;

import com.github.kfcfans.powerjob.common.WorkflowContextConstant;
import com.github.kfcfans.powerjob.common.utils.JsonUtils;
import com.github.kfcfans.powerjob.worker.core.processor.ProcessResult;
import com.github.kfcfans.powerjob.worker.core.processor.TaskContext;
import com.github.kfcfans.powerjob.worker.core.processor.sdk.BasicProcessor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 测试追加工作流上下文数据
 * com.github.kfcfans.powerjob.samples.tester.AppendWorkflowContextTester
 *
 * @author Echo009
 * @since 2021/2/6
 */
@Component
public class AppendWorkflowContextTester implements BasicProcessor {


    @Override
    @SuppressWarnings("squid:S106")
    public ProcessResult process(TaskContext context) throws Exception {

        Map<String, String> workflowContext = context.fetchWorkflowContext();
        String originValue = workflowContext.get(WorkflowContextConstant.CONTEXT_INIT_PARAMS_KEY);
        System.out.println("======= AppendWorkflowContextTester#start =======");
        System.out.println("current instance id : " + context.getInstanceId());
        System.out.println("current workflow context : " + workflowContext);
        System.out.println("initParam of workflow context : " + originValue);
        int num = 0;
        try {
            num = Integer.parseInt(originValue);
        } catch (Exception e) {
            // ignore
        }
        context.appendData2WfContext(WorkflowContextConstant.CONTEXT_INIT_PARAMS_KEY, num + 1);
        System.out.println("======= AppendWorkflowContextTester#end =======");
        return new ProcessResult(true, JsonUtils.toJSONString(context));
    }
}
