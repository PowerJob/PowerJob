package tech.powerjob.worker.processor.impl;

import org.apache.commons.lang3.exception.ExceptionUtils;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class MethodBasicProcessor implements BasicProcessor {

    private final Object bean;

    private final Method method;

    public MethodBasicProcessor(Object bean, Method method) {
        this.bean = bean;
        this.method = method;
    }

    @Override
    public ProcessResult process(TaskContext context) throws Exception {
        try {
            Object result = method.invoke(bean, context);
            return new ProcessResult(true, JsonUtils.toJSONString(result));
        } catch (InvocationTargetException ite) {
            ExceptionUtils.rethrow(ite.getTargetException());
        }

        return new ProcessResult(false, "IMPOSSIBLE");
    }
}
