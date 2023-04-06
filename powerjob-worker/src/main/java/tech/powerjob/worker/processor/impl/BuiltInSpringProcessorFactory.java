package tech.powerjob.worker.processor.impl;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import tech.powerjob.common.enums.ProcessorType;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;
import tech.powerjob.worker.extension.processor.ProcessorBean;
import tech.powerjob.worker.extension.processor.ProcessorDefinition;
import tech.powerjob.worker.extension.processor.ProcessorFactory;

import java.util.Set;

/**
 * 内建的 SpringBean 处理器工厂，用于加载 Spring 相关的Bean，非核心依赖
 *
 * @author tjq
 * @since 2023/1/17
 */
@Slf4j
public class BuiltInSpringProcessorFactory implements ProcessorFactory {

    private final ApplicationContext applicationContext;

    public BuiltInSpringProcessorFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public Set<String> supportTypes() {
        return Sets.newHashSet(ProcessorType.BUILT_IN.name());
    }

    @Override
    public ProcessorBean build(ProcessorDefinition processorDefinition) {

        try {
            boolean canLoad = checkCanLoad();
            if (!canLoad) {
                log.info("[ProcessorFactory] can't find Spring env, this processor can't load by 'BuiltInSpringProcessorFactory'");
                return null;
            }
            String processorInfo = processorDefinition.getProcessorInfo();
            //用于区分方法级别的参数
            if (processorInfo.contains("#")) {
                return null;
            }
            BasicProcessor basicProcessor = getBean(processorInfo, applicationContext);
            return new ProcessorBean()
                    .setProcessor(basicProcessor)
                    .setClassLoader(basicProcessor.getClass().getClassLoader());
        } catch (NoSuchBeanDefinitionException ignore) {
            log.warn("[ProcessorFactory] can't find the processor in SPRING");
        } catch (Throwable t) {
            log.warn("[ProcessorFactory] load by BuiltInSpringProcessorFactory failed. If you are using Spring, make sure this bean was managed by Spring", t);
        }

        return null;
    }

    private boolean checkCanLoad() {
        try {
            ApplicationContext.class.getClassLoader();
            return applicationContext != null;
        } catch (Throwable ignore) {
        }
        return false;
    }


    @SuppressWarnings("unchecked")
    private static <T> T getBean(String className, ApplicationContext ctx) throws Exception {

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
