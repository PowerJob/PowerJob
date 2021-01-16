package com.github.kfcfans.powerjob.samples.tester;

import com.github.kfcfans.powerjob.worker.core.processor.ProcessResult;
import com.github.kfcfans.powerjob.worker.core.processor.TaskContext;
import com.github.kfcfans.powerjob.worker.core.processor.sdk.BasicProcessor;
import org.springframework.stereotype.Component;

/**
 * 测试用户反馈的无法停止实例的问题
 * https://github.com/PowerJob/PowerJob/issues/37
 *
 * @author tjq
 * @since 2020/7/30
 */
@Component
public class StopInstanceTester implements BasicProcessor {
    @Override
    public ProcessResult process(TaskContext context) throws Exception {
        int i = 0;
        while (true) {
            System.out.println(i++);
            Thread.sleep(1000*10);
        }
    }
}
