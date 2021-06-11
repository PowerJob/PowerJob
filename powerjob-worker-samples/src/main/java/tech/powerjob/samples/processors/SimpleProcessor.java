package tech.powerjob.samples.processors;

import org.apache.commons.lang3.StringUtils;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;
import tech.powerjob.worker.log.OmsLogger;

/**
 * SimpleProcessor
 *
 * @author Echo009
 * @since 2021/2/6
 */
public class SimpleProcessor implements BasicProcessor {


    @Override
    public ProcessResult process(TaskContext context) throws Exception {

        OmsLogger logger = context.getOmsLogger();

        String jobParams = context.getJobParams();
        logger.info("Current context:{}", context.getWorkflowContext());
        logger.info("Current job params:{}", jobParams);

        return jobParams.contains("F") ? new ProcessResult(false) : new ProcessResult(true);

    }
}
