package tech.powerjob.worker.processor;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.worker.extension.processor.ProcessorBean;
import tech.powerjob.worker.extension.processor.ProcessorDefinition;
import tech.powerjob.worker.extension.processor.ProcessorFactory;

import java.util.List;
import java.util.Map;

/**
 * PowerJobProcessorLoader
 *
 * @author tjq
 * @since 2023/1/17
 */
@Slf4j
public class PowerJobProcessorLoader {

    private final List<ProcessorFactory> processorFactoryList;
    private final Map<ProcessorDefinition, ProcessorBean> def2Bean = Maps.newConcurrentMap();

    public PowerJobProcessorLoader(List<ProcessorFactory> processorFactoryList) {
        this.processorFactoryList = processorFactoryList;
    }

    public ProcessorBean load(ProcessorDefinition definition) {
        return def2Bean.computeIfAbsent(definition, ignore -> {
            for (ProcessorFactory pf : processorFactoryList) {
                try {
                    ProcessorBean processorBean = pf.build(definition);
                    if (processorBean != null) {
                        return processorBean;
                    }
                } catch (Throwable t) {
                    log.error("[ProcessorFactory] [{}] load processor failed: {}", pf.getClass().getSimpleName(), definition, t);
                }
            }
            throw new PowerJobException("fetch Processor failed, please check your processorType and processorInfo config");
        });
    }
}
