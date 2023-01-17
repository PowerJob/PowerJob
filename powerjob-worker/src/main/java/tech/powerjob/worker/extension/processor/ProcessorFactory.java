package tech.powerjob.worker.extension.processor;

/**
 * 处理器工厂
 * 考虑到当前是一个百花齐放的生态，各种 IOC 框架层出不穷。PowerJob 决定在 4.3.0 剥离对 Spring 的强依赖，并允许开发者自定义 Bean 的初始化逻辑
 *
 * @author tjq
 * @since 2023/1/17
 */
public interface ProcessorFactory {

    /**
     * 根据处理器定义构建处理器对象
     * 注意：Processor 为单例对象，即 PowerJob 对每一个 ProcessorBean 只调用一次 build 方法
     * @param processorDefinition 处理器定义
     * @return null or ProcessorBean
     */
    ProcessorBean build(ProcessorDefinition processorDefinition);
}
