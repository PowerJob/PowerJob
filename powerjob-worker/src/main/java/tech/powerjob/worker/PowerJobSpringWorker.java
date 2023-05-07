package tech.powerjob.worker;

import com.google.common.collect.Lists;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import tech.powerjob.worker.common.PowerJobWorkerConfig;
import tech.powerjob.worker.extension.processor.ProcessorFactory;
import tech.powerjob.worker.processor.impl.BuildInSpringMethodProcessorFactory;
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
public class PowerJobSpringWorker implements ApplicationContextAware, InitializingBean, DisposableBean {

    /**
     * 组合优于继承，持有 PowerJobWorker，内部重新设置 ProcessorFactory 更优雅
     */
    private PowerJobWorker powerJobWorker;
    private final PowerJobWorkerConfig config;

    public PowerJobSpringWorker(PowerJobWorkerConfig config) {
        this.config = config;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        powerJobWorker = new PowerJobWorker(config);
        powerJobWorker.init();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        BuiltInSpringProcessorFactory springProcessorFactory = new BuiltInSpringProcessorFactory(applicationContext);

        BuildInSpringMethodProcessorFactory springMethodProcessorFactory = new BuildInSpringMethodProcessorFactory(applicationContext);
        // append BuiltInSpringProcessorFactory

        List<ProcessorFactory> processorFactories = Lists.newArrayList(
                Optional.ofNullable(config.getProcessorFactoryList())
                        .orElse(Collections.emptyList()));
        processorFactories.add(springProcessorFactory);
        processorFactories.add(springMethodProcessorFactory);
        config.setProcessorFactoryList(processorFactories);
    }

    @Override
    public void destroy() throws Exception {
        powerJobWorker.destroy();
    }
}
