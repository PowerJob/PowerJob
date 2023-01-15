package tech.powerjob.worker.core.processor;

import akka.actor.ActorSelection;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import tech.powerjob.common.enums.ProcessorType;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.worker.common.WorkerRuntime;
import tech.powerjob.worker.common.utils.AkkaUtils;
import tech.powerjob.worker.common.utils.SpringUtils;
import tech.powerjob.worker.container.OmsContainer;
import tech.powerjob.worker.container.OmsContainerFactory;
import tech.powerjob.worker.core.ProcessorBeanFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Echo009
 * @since 2022/9/19
 */
@Slf4j
public class ProcessorLoader {


    private static final Map<String, ProcessorInfo> CACHE;


    static {
        // init
        CACHE = new ConcurrentHashMap<>(128);
    }

    /**
     * 获取处理器
     * @param workerRuntime 运行时
     * @param processorType 处理器类型
     * @param processorInfo   处理器 id ，一般是全限定类名
     * @return processor
     */
    public static ProcessorInfo loadProcessor(WorkerRuntime workerRuntime, String processorType, String processorInfo) {
        ProcessorInfo processorInfoHolder = null;
        ProcessorType type = ProcessorType.valueOf(processorType);

        switch (type) {
            case BUILT_IN:
                // 先从缓存中取
                processorInfoHolder = CACHE.computeIfAbsent(processorInfo, ignore -> {
                    // 先使用 Spring 加载
                    if (SpringUtils.supportSpringBean()) {
                        try {
                            return  ProcessorInfo.of(SpringUtils.getBean(processorInfo),workerRuntime.getClass().getClassLoader());
                        } catch (Exception e) {
                            log.warn("[ProcessorLoader] no spring bean of processor(className={}), reason is {}.", processorInfo, ExceptionUtils.getMessage(e));
                        }
                    }
                    // 反射加载
                    return  ProcessorInfo.of(ProcessorBeanFactory.getInstance().getLocalProcessor(processorInfo),workerRuntime.getClass().getClassLoader());
                });
                break;
            case EXTERNAL:
                String[] split = processorInfo.split("#");
                log.info("[ProcessorLoader] try to load processor({}) in container({})", split[1], split[0]);

                String serverPath = AkkaUtils.getServerActorPath(workerRuntime.getServerDiscoveryService().getCurrentServerAddress());
                ActorSelection actorSelection = workerRuntime.getActorSystem().actorSelection(serverPath);
                OmsContainer omsContainer = OmsContainerFactory.fetchContainer(Long.valueOf(split[0]), actorSelection);
                if (omsContainer != null) {
                    processorInfoHolder = ProcessorInfo.of(omsContainer.getProcessor(split[1]), omsContainer.getContainerClassLoader());
                } else {
                    log.warn("[ProcessorLoader] load container failed. processor info : {}", processorInfo);
                }
                break;
            default:
                log.warn("[ProcessorLoader] unknown processor type: {}.", processorType);
                throw new PowerJobException("unknown processor type of " + processorType);
        }

        if (processorInfoHolder == null) {
            log.warn("[ProcessorLoader] fetch Processor(type={},info={}) failed.", processorType, processorInfo);
            throw new PowerJobException("fetch Processor failed, please check your processorType and processorInfo config");
        }

        return processorInfoHolder;

    }


}
