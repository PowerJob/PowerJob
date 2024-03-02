package tech.powerjob.worker.processor.impl;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import tech.powerjob.common.enums.ProcessorType;
import tech.powerjob.worker.common.WorkerRuntime;
import tech.powerjob.worker.container.OmsContainer;
import tech.powerjob.worker.container.OmsContainerFactory;
import tech.powerjob.worker.extension.processor.ProcessorBean;
import tech.powerjob.worker.extension.processor.ProcessorDefinition;
import tech.powerjob.worker.extension.processor.ProcessorFactory;

import java.util.Set;

/**
 * 加载容器处理器
 *
 * @author tjq
 * @since 2023/1/17
 */
@Slf4j
public class JarContainerProcessorFactory implements ProcessorFactory {

    private final WorkerRuntime workerRuntime;

    public JarContainerProcessorFactory(WorkerRuntime workerRuntime) {
        this.workerRuntime = workerRuntime;
    }

    @Override
    public Set<String> supportTypes() {
        return Sets.newHashSet(ProcessorType.EXTERNAL.name());
    }

    @Override
    public ProcessorBean build(ProcessorDefinition processorDefinition) {

        String processorInfo = processorDefinition.getProcessorInfo();
        String[] split = processorInfo.split("#");
        String containerName = split[0];
        String className = split[1];

        log.info("[ProcessorFactory] try to load processor({}) in container({})", className, containerName);

        OmsContainer omsContainer = OmsContainerFactory.fetchContainer(Long.valueOf(containerName), workerRuntime);
        if (omsContainer != null) {
            return new ProcessorBean()
                    .setProcessor(omsContainer.getProcessor(className))
                    .setClassLoader(omsContainer.getContainerClassLoader())
                    .setStable(false)
                    ;
        } else {
            log.warn("[ProcessorFactory] load container failed. processor info : {}", processorInfo);
        }
        return null;
    }
}
