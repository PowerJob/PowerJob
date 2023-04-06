package tech.powerjob.samples.tester;

import org.springframework.stereotype.Component;
import tech.powerjob.worker.annotation.PowerJob;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.log.OmsLogger;

@Component
public class SpringMethodProcessorService {

    @PowerJob("test")
    public void test(TaskContext context) {
        OmsLogger omsLogger = context.getOmsLogger();
        omsLogger.warn("测试日志");
        System.out.println("测试执行");
    }
}
