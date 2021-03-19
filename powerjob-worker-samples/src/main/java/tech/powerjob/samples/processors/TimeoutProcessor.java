package tech.powerjob.samples.processors;

import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;
import org.springframework.stereotype.Component;

/**
 * 测试超时任务
 *
 * @author tjq
 * @since 2020/4/20
 */
@Component
public class TimeoutProcessor implements BasicProcessor {
    @Override
    public ProcessResult process(TaskContext context) throws Exception {
        Thread.sleep(Long.parseLong(context.getJobParams()));
        return new ProcessResult(true, "impossible~~~~QAQ~");
    }
}
