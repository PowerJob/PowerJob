package tech.powerjob.official.processors.impl.context;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import tech.powerjob.official.processors.CommonBasicProcessor;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.WorkflowContext;
import tech.powerjob.worker.log.OmsLogger;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Echo009
 * @since 2022/2/16
 */
public class InjectWorkflowContextProcessor extends CommonBasicProcessor {


    @Override
    protected ProcessResult process0(TaskContext taskContext) {

        String jobParams = taskContext.getJobParams();
        OmsLogger omsLogger = taskContext.getOmsLogger();
        try {
            HashMap<String, Object> data = JSON.parseObject(jobParams, new TypeReference<HashMap<String, Object>>() {
            });
            WorkflowContext workflowContext = taskContext.getWorkflowContext();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                workflowContext.appendData2WfContext(entry.getKey(), entry.getValue());
                omsLogger.info("inject context, {}:{}", entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            omsLogger.error("Fail to parse job params:{},it is not a valid json string!", jobParams, e);
            return new ProcessResult(false);
        }
        return new ProcessResult(true);
    }
}
