package tech.powerjob.samples.tester;

import org.springframework.stereotype.Component;
import tech.powerjob.worker.annotation.PowerJob;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.log.OmsLogger;

@Component
public class SpringMethodProcessorService {

    @PowerJob("test")
    public String test(TaskContext context) {
        OmsLogger omsLogger = context.getOmsLogger();
        omsLogger.warn("测试日志");
        return null;
    }


    @PowerJob("test1")
    public String test1(TaskContext context) {
        OmsLogger omsLogger = context.getOmsLogger();
        omsLogger.warn("测试日志");
        return "测试日志";
    }
}
