package tech.powerjob.worker;

import com.google.common.collect.Lists;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import tech.powerjob.worker.common.PowerJobWorkerConfig;
import tech.powerjob.worker.extension.processor.ProcessorFactory;
import tech.powerjob.worker.processor.impl.BuiltInSpringProcessorFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Spring 项目中的 Worker 启动器
 * 能够获取到由 Spring IOC 容器管理的 processor
 *
 * @author tjq
 * @since 2023/1/20
 */
public class PowerJobSpringWorker extends PowerJobWorker implements ApplicationContextAware, InitializingBean, DisposableBean {


    public PowerJobSpringWorker(PowerJobWorkerConfig config) {
        super(config);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        BuiltInSpringProcessorFactory springProcessorFactory = new BuiltInSpringProcessorFactory(applicationContext);

        // append BuiltInSpringProcessorFactory
        PowerJobWorkerConfig workerConfig = workerRuntime.getWorkerConfig();
        List<ProcessorFactory> processorFactories = Lists.newArrayList(
                Optional.ofNullable(workerConfig.getProcessorFactoryList())
                        .orElse(Collections.emptyList()));
        processorFactories.add(springProcessorFactory);
        workerConfig.setProcessorFactoryList(processorFactories);
    }

}
