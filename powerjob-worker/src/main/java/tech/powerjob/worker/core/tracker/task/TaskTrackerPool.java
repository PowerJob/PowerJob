package tech.powerjob.worker.core.tracker.task;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 持有 TaskTracker 对象
 *
 * @author tjq
 * @since 2020/3/24
 */
public class TaskTrackerPool {

    private static final Map<Long, TaskTracker> instanceId2TaskTracker = Maps.newConcurrentMap();

    /**
     * 获取 TaskTracker
     */
    public static TaskTracker getTaskTrackerPool(Long instanceId) {
        return instanceId2TaskTracker.get(instanceId);
    }

    public static TaskTracker remove(Long instanceId) {
        return instanceId2TaskTracker.remove(instanceId);
    }

    public static void atomicCreateTaskTracker(Long instanceId, Function<Long, TaskTracker> creator) {
        instanceId2TaskTracker.computeIfAbsent(instanceId, creator);
    }

    public static List<Long> getAllFrequentTaskTrackerKeys() {
        List<Long> keys = Lists.newLinkedList();
        instanceId2TaskTracker.forEach((key, tk) -> {
            if (tk instanceof FrequentTaskTracker) {
                keys.add(key);
            }
        });
        return keys;
    }

}
