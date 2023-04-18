package tech.powerjob.worker.processor;

import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

import java.lang.reflect.Method;

public class MethodBasicProcessor implements BasicProcessor {

    private final Object bean;

    private final Method method;

    public MethodBasicProcessor(Object bean, Method method) {
        this.bean = bean;
        this.method = method;
    }

    @Override
    public ProcessResult process(TaskContext context) throws Exception {
        Object result = method.invoke(bean, context);
        return new ProcessResult(true, JsonUtils.toJSONString(result));
    }
}
