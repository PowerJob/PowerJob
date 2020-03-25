package com.github.kfcfans.oms.worker.core.tracker.task;

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.function.Function;

/**
 * 持有 Processor 对象
 *
 * @author tjq
 * @since 2020/3/24
 */
public class TaskTrackerPool {

    private static final Map<String, TaskTracker> instanceId2TaskTracker = Maps.newConcurrentMap();

    /**
     * 获取 ProcessorTracker，如果不存在则创建
     */
    public static TaskTracker getTaskTrackerPool(String instanceId) {
        return instanceId2TaskTracker.get(instanceId);
    }

    public static void remove(String instanceId) {
        instanceId2TaskTracker.remove(instanceId);
    }

    public static void atomicCreateTaskTracker(String instanceId, Function<String, TaskTracker> creator) {
        instanceId2TaskTracker.computeIfAbsent(instanceId, creator);
    }

}
