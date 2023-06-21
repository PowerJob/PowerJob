package tech.powerjob.samples.processors.test;

import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

/**
 * ZeroCostTestProcessor
 *
 * @author tjq
 * @since 2023/5/7
 */
public class ZeroCostTestProcessor implements BasicProcessor {
    @Override
    public ProcessResult process(TaskContext context) throws Exception {
        return new ProcessResult(true, "zero cost");
    }
}
