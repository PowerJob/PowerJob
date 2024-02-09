package tech.powerjob.samples.tester;

import org.springframework.stereotype.Component;
import tech.powerjob.samples.anno.ATestMethodAnnotation;
import tech.powerjob.worker.annotation.PowerJobHandler;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.log.OmsLogger;

@Component(value = "springMethodProcessorService")
public class SpringMethodProcessorService {

    /**
     * 处理器配置方法1： 全限定类名#方法名，比如 tech.powerjob.samples.tester.SpringMethodProcessorService#testEmptyReturn
     * 处理器配置方法2： SpringBean名称#方法名，比如 springMethodProcessorService#testEmptyReturn
     * @param context 必须要有入参 TaskContext，返回值可以是 null，也可以是其他任意类型。正常返回代表成功，抛出异常代表执行失败
     */
    @PowerJobHandler(name = "testEmptyReturn")
    public void testEmptyReturn(TaskContext context) {
        OmsLogger omsLogger = context.getOmsLogger();
        omsLogger.warn("测试日志");
    }


    @PowerJobHandler(name = "testNormalReturn")
    public String testNormalReturn(TaskContext context) {
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

    @ATestMethodAnnotation
    @PowerJobHandler(name = "testNormalReturnWithCustomAnno")
    public String testNormalReturnWithCustomAnno(TaskContext context) {
        OmsLogger omsLogger = context.getOmsLogger();
        omsLogger.warn("测试自定义注解");
        return "testNormalReturnWithCustomAnno";
    }
}
