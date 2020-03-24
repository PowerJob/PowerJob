package com.github.kfcfans.oms.worker.core.tracker.processor;

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.function.Function;

/**
 * 持有 Processor 对象
 * instanceId -> Processor
 *
 * @author tjq
 * @since 2020/3/20
 */
public class ProcessorTrackerPool {

    private static final Map<String, ProcessorTracker> instanceId2ProcessorTracker = Maps.newConcurrentMap();

    /**
     * 获取 ProcessorTracker，如果不存在则创建
     */
    public static ProcessorTracker getProcessorTracker(String instanceId, Function<String, ProcessorTracker> creator) {
        return instanceId2ProcessorTracker.computeIfAbsent(instanceId, creator);
    }
}
