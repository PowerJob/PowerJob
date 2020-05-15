package com.github.kfcfans.oms.samples.processors;

import com.github.kfcfans.oms.worker.core.processor.ProcessResult;
import com.github.kfcfans.oms.worker.core.processor.TaskContext;
import com.github.kfcfans.oms.worker.core.processor.sdk.BasicProcessor;
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
