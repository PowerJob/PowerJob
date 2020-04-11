package com.github.kfcfans.oms.worker.core.classloader;

import com.github.kfcfans.oms.worker.sdk.api.BasicProcessor;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.Map;

/**
 * 处理器工厂
 *
 * @author tjq
 * @since 2020/3/23
 */
@Slf4j
public class ProcessorBeanFactory {

    private final OhMyClassLoader ohMyClassLoader;
    // key（用来防止不同jar包同名类的冲突） -> (className -> Processor)
    private final Map<String, Map<String, BasicProcessor>> cache;
    private static final String LOCAL_KEY = "local";

    private static volatile ProcessorBeanFactory processorBeanFactory;

    public ProcessorBeanFactory() {

        // 1. 初始化类加载器
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        URL path = contextClassLoader.getResource("");
        ohMyClassLoader = new OhMyClassLoader(new URL[]{path}, contextClassLoader);

        // 2. 初始化对象缓存
        cache = Maps.newConcurrentMap();
        Map<String, BasicProcessor> className2Processor = Maps.newConcurrentMap();
        cache.put(LOCAL_KEY, className2Processor);
    }

    public BasicProcessor getLocalProcessor(String className) {
        return cache.get(LOCAL_KEY).computeIfAbsent(className, ignore -> {
            try {

                Class<?> clz = ohMyClassLoader.loadClass(className);
                BasicProcessor processor = (BasicProcessor) clz.getDeclaredConstructor().newInstance();
                processor.init();

                return processor;

            }catch (Exception e) {
                log.error("[ProcessorBeanFactory] load local Processor(className = {}) failed.", className, e);
            }
            return null;
        });
    }



    public static ProcessorBeanFactory getInstance() {
        if (processorBeanFactory != null) {
            return processorBeanFactory;
        }
        synchronized (ProcessorBeanFactory.class) {
            if (processorBeanFactory == null) {
                processorBeanFactory = new ProcessorBeanFactory();
            }
        }
        return processorBeanFactory;
    }
}
