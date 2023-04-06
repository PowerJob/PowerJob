package tech.powerjob.worker.processor.impl;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import tech.powerjob.common.enums.ProcessorType;
import tech.powerjob.worker.annotation.PowerJob;
import tech.powerjob.worker.extension.processor.ProcessorBean;
import tech.powerjob.worker.extension.processor.ProcessorDefinition;
import tech.powerjob.worker.extension.processor.ProcessorFactory;
import tech.powerjob.worker.processor.MethodBasicProcessor;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * 内建的 SpringBean 处理器工厂，用于加载 Spring 管理Bean下的方法（使用PowerJob注解），非核心依赖
 *
 * @author wxp
 * @since 2023/4/06
 */

@Slf4j
public class BuildInSpringMethodProcessorFactory implements ProcessorFactory {

    private final ApplicationContext applicationContext;

    private static final List<String> jobHandlerRepository = new LinkedList<>();

    private final static String DELIMITER = "#";


    public BuildInSpringMethodProcessorFactory(ApplicationContext applicationContext) {
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
                log.info("[ProcessorFactory] can't find Spring env, this processor can't load by 'BuildInSpringMethodProcessorFactory'");
                return null;
            }
            String processorInfo = processorDefinition.getProcessorInfo();
            if (!processorInfo.contains(DELIMITER)) {
                log.info("[ProcessorFactory] can't parse processorDefinition, this processor can't load by 'BuildInSpringMethodProcessorFactory'");
                return null;
            }
            String[] split = processorInfo.split("#");
            String methodName = split[1];
            String className = split[0];
            Object bean = getBean(className,applicationContext);
            Method[] methods = bean.getClass().getDeclaredMethods();
            for (Method method : methods) {
                PowerJob powerJob = method.getAnnotation(PowerJob.class);
                if (powerJob == null) {
                    continue;
                }
                String name = powerJob.value();
                //匹配到和页面定义相同的methodName
                if (!name.equals(methodName)) {
                    continue;
                }
                if (name.trim().length() == 0) {
                    throw new RuntimeException("method-jobhandler name invalid, for[" + bean.getClass() + "#" + method.getName() + "] .");
                }
                if (containsJobHandler(name)) {
                    throw new RuntimeException("jobhandler[" + name + "] naming conflicts.");
                }
                method.setAccessible(true);
                registerJobHandler(methodName);

                MethodBasicProcessor processor = new MethodBasicProcessor(bean, method);
                return new ProcessorBean()
                        .setProcessor(processor)
                        .setClassLoader(processor.getClass().getClassLoader());
            }
        } catch (NoSuchBeanDefinitionException ignore) {
            log.warn("[ProcessorFactory] can't find the processor in SPRING");
        } catch (Throwable t) {
            log.warn("[ProcessorFactory] load by BuiltInSpringProcessorFactory failed. If you are using Spring, make sure this bean was managed by Spring", t);
        }
        return null;

    }


    public static void registerJobHandler(String name) {
        jobHandlerRepository.add(name);
    }


    private boolean containsJobHandler(String name) {
        return jobHandlerRepository.contains(name);
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
    private static Object getBean(String className, ApplicationContext ctx) throws Exception {

        // 0. 尝试直接用 Bean 名称加载
        try {
            final Object bean = ctx.getBean(className);
            if (bean != null) {
                return bean;
            }
        } catch (Exception ignore) {
        }

        // 1. ClassLoader 存在，则直接使用 clz 加载
        ClassLoader classLoader = ctx.getClassLoader();
        if (classLoader != null) {
            return ctx.getBean(classLoader.loadClass(className));
        }
        // 2. ClassLoader 不存在(系统类加载器不可见)，尝试用类名称小写加载
        String[] split = className.split("\\.");
        String beanName = split[split.length - 1];
        // 小写转大写
        char[] cs = beanName.toCharArray();
        cs[0] += 32;
        String beanName0 = String.valueOf(cs);
        log.warn("[SpringUtils] can't get ClassLoader from context[{}], try to load by beanName:{}", ctx, beanName0);
        return ctx.getBean(beanName0);
    }


}
