package tech.powerjob.samples.tester;

import tech.powerjob.common.WorkflowContextConstant;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 测试追加工作流上下文数据
 *
 * @author Echo009
 * @since 2021/2/6
 */
@Component
public class AppendWorkflowContextTester implements BasicProcessor {

    private static final String FAIL_CODE = "0";

    @Override
    @SuppressWarnings("squid:S106")
    public ProcessResult process(TaskContext context) throws Exception {

        Map<String, String> workflowContext = context.getWorkflowContext().fetchWorkflowContext();
        String originValue = workflowContext.get(WorkflowContextConstant.CONTEXT_INIT_PARAMS_KEY);
        System.out.println("======= AppendWorkflowContextTester#start =======");
        System.out.println("current instance id : " + context.getInstanceId());
        System.out.println("current workflow context : " + workflowContext);
        System.out.println("current job param : " + context.getJobParams());
        System.out.println("initParam of workflow context : " + originValue);
        int num = 0;
        try {
            num = Integer.parseInt(originValue);
        } catch (Exception e) {
            // ignore
        }
        context.getWorkflowContext().appendData2WfContext(WorkflowContextConstant.CONTEXT_INIT_PARAMS_KEY, num + 1);
        System.out.println("======= AppendWorkflowContextTester#end =======");
        if (FAIL_CODE.equals(context.getJobParams())) {
            return new ProcessResult(false, "Failed!");
        }
        return new ProcessResult(true, "Success!");
    }
}
