package tech.powerjob.worker.processor.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import tech.powerjob.worker.annotation.PowerJobHandler;
import tech.powerjob.worker.extension.processor.ProcessorBean;
import tech.powerjob.worker.extension.processor.ProcessorDefinition;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

/**
 * 内建的 SpringBean 处理器工厂，用于加载 Spring 管理Bean下的方法（使用PowerJob注解），非核心依赖
 *
 * @author wxp
 * @since 2023/4/06
 */

@Slf4j
public class BuildInSpringMethodProcessorFactory extends AbstractBuildInSpringProcessorFactory {

    private static final List<String> jobHandlerRepository = new LinkedList<>();

    private final static String DELIMITER = "#";


    public BuildInSpringMethodProcessorFactory(ApplicationContext applicationContext) {
        super(applicationContext);
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
            String[] split = processorInfo.split(DELIMITER);
            String methodName = split[1];
            String className = split[0];
            Object bean = getBean(className,applicationContext);
            Method[] methods = bean.getClass().getDeclaredMethods();
            for (Method method : methods) {
                PowerJobHandler powerJob = method.getAnnotation(PowerJobHandler.class);

                // CGLib代理对象拿不到该注解, 通过 AnnotationUtils.findAnnotation()可以获取到注解 by GitHub@zhangxiang0907 https://github.com/PowerJob/PowerJob/issues/770
                if (powerJob == null) {
                    powerJob = AnnotationUtils.findAnnotation(method, PowerJobHandler.class);
                }

                if (powerJob == null) {
                    continue;
                }
                String name = powerJob.name();
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


}
