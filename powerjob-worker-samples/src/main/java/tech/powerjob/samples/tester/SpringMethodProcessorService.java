package tech.powerjob.samples.tester;

import org.springframework.stereotype.Component;
import tech.powerjob.worker.annotation.PowerJobHandler;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.log.OmsLogger;

@Component
public class SpringMethodProcessorService {

    @PowerJobHandler(name = "testEmptyReturn")
    public void test(TaskContext context) {
        OmsLogger omsLogger = context.getOmsLogger();
        omsLogger.warn("测试日志");
    }


    @PowerJobHandler(name = "testNormalReturn")
    public String test1(TaskContext context) {
        OmsLogger omsLogger = context.getOmsLogger();
        omsLogger.warn("测试日志");
        return "testNormalReturn";
    }

    @PowerJobHandler(name = "testThrowException")
    public String testThrowException(TaskContext context) {
        OmsLogger omsLogger = context.getOmsLogger();
        omsLogger.warn("testThrowException");
        throw new IllegalArgumentException("test");
    }
}
