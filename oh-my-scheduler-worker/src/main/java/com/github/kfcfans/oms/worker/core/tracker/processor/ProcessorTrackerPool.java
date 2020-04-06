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

    private static final Map<Long, ProcessorTracker> instanceId2ProcessorTracker = Maps.newConcurrentMap();

    /**
     * 获取 ProcessorTracker，如果不存在则创建
     */
    public static ProcessorTracker getProcessorTracker(Long instanceId, Function<Long, ProcessorTracker> creator) {
        return instanceId2ProcessorTracker.computeIfAbsent(instanceId, creator);
    }

    /**
     * 获取 ProcessorTracker
     */
    public static ProcessorTracker getProcessorTracker(Long instanceId) {
        return instanceId2ProcessorTracker.get(instanceId);
    }

    public static void removeProcessorTracker(Long instanceId) {
        instanceId2ProcessorTracker.remove(instanceId);
    }
}
