package tech.powerjob.worker.processor.impl;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import tech.powerjob.common.enums.ProcessorType;
import tech.powerjob.worker.extension.processor.ProcessorFactory;

import java.util.Set;

@Slf4j
public abstract class AbstractBuildInSpringProcessorFactory implements ProcessorFactory {

    protected final ApplicationContext applicationContext;

    protected AbstractBuildInSpringProcessorFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public Set<String> supportTypes() {
        return Sets.newHashSet(ProcessorType.BUILT_IN.name());
    }

    protected boolean checkCanLoad() {
        try {
            ApplicationContext.class.getClassLoader();
            return applicationContext != null;
        } catch (Throwable ignore) {
        }
        return false;
    }


    @SuppressWarnings("unchecked")
    protected static <T> T getBean(String className, ApplicationContext ctx) throws Exception {

        // 0. 尝试直接用 Bean 名称加载
        try {
            final Object bean = ctx.getBean(className);
            if (bean != null) {
                return (T) bean;
            }
        } catch (Exception ignore) {
        }

        // 1. ClassLoader 存在，则直接使用 clz 加载
        ClassLoader classLoader = ctx.getClassLoader();
        if (classLoader != null) {
            return (T) ctx.getBean(classLoader.loadClass(className));
        }
        // 2. ClassLoader 不存在(系统类加载器不可见)，尝试用类名称小写加载
        String[] split = className.split("\\.");
        String beanName = split[split.length - 1];
        // 小写转大写
        char[] cs = beanName.toCharArray();
        cs[0] += 32;
        String beanName0 = String.valueOf(cs);
        log.warn("[SpringUtils] can't get ClassLoader from context[{}], try to load by beanName:{}", ctx, beanName0);
        return (T) ctx.getBean(beanName0);
    }



}
