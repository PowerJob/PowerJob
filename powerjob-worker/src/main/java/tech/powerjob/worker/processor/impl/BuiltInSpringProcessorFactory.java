package tech.powerjob.worker.processor.impl;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import tech.powerjob.common.enums.ProcessorType;
import tech.powerjob.worker.common.utils.SpringUtils;
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

        log.info("[ProcessorFactory] use 'BuiltInSpringProcessorFactory' to load, processorDefinition is: {}", processorDefinition);
        try {
            boolean canLoad = checkCanLoad();
            if (!canLoad) {
                log.info("[ProcessorFactory] can't find Spring env, this processor can't load by 'BuiltInSpringProcessorFactory'");
                return null;
            }

            BasicProcessor basicProcessor = SpringUtils.getBean(processorDefinition.getProcessorInfo(), applicationContext);
            return new ProcessorBean()
                    .setProcessor(basicProcessor)
                    .setClassLoader(basicProcessor.getClass().getClassLoader());
        } catch (Throwable t) {
            log.warn("[ProcessorFactory] load by BuiltInSpringProcessorFactory failed!", t);
        }

        return null;
    }

    private boolean checkCanLoad() {
        if (SpringUtils.inSpringEnv()) {
            return applicationContext != null;
        }
        return false;
    }

}
