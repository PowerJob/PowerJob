package tech.powerjob.worker.extension.processor;

import tech.powerjob.common.enums.ProcessorType;

import java.util.Set;

/**
 * 处理器工厂
 * 考虑到当前是一个百花齐放的生态，各种 IOC 框架层出不穷。PowerJob 决定在 4.3.0 剥离对 Spring 的强依赖，并允许开发者自定义 Bean 的初始化逻辑
 * 不知道怎么用的话，可以看看官方提供的 3 个默认实现，比如对接第三方 IOC 框架就类似于 BuiltInSpringProcessorFactory
 *
 * @author tjq
 * @since 2023/1/17
 */
public interface ProcessorFactory {

    /**
     * 支持的处理器类型，类型不匹配则跳过该 ProcessorFactory 的加载逻辑
     * 对应的是控制台的'处理器类型' TAB，不做任何定制的情况下，取值范围为 {@link ProcessorType#name()}
     * @return 支持的处理器类型
     */
    Set<String> supportTypes();

    /**
     * 根据处理器定义构建处理器对象
     * 注意：Processor 为单例对象，即 PowerJob 对每一个 ProcessorBean 只调用一次 build 方法
     * @param processorDefinition 处理器定义
     * @return null or ProcessorBean
     */
    ProcessorBean build(ProcessorDefinition processorDefinition);
}
