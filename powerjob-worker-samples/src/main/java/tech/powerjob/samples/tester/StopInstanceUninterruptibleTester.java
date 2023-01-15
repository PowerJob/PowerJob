package tech.powerjob.samples.tester;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

/**
 * 停止实例 (不可中断)
 *
 * @author Echo009
 * @since 2023/1/15
 */
@Component
@Slf4j
@SuppressWarnings("all")
public class StopInstanceUninterruptibleTester implements BasicProcessor {
    @Override
    public ProcessResult process(TaskContext context) throws Exception {
        int i = 0;
        while (true) {
            //  uninterruptible
            i++;
            if (i % 1000000000 == 0){
                log.info("taskInstance({}) is running ...",context.getInstanceId());
            }
        }
    }
}
