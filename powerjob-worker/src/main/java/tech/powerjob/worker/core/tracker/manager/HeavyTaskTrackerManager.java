package tech.powerjob.worker.core.tracker.manager;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import tech.powerjob.worker.core.tracker.task.heavy.FrequentTaskTracker;
import tech.powerjob.worker.core.tracker.task.heavy.HeavyTaskTracker;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 持有 TaskTracker 对象
 *
 * @author tjq
 * @since 2020/3/24
 */
public class HeavyTaskTrackerManager {

    private static final Map<Long, HeavyTaskTracker> INSTANCE_ID_2_TASK_TRACKER = Maps.newConcurrentMap();
    /**
     * 获取 TaskTracker
     */
    public static HeavyTaskTracker getTaskTracker(Long instanceId) {
        return INSTANCE_ID_2_TASK_TRACKER.get(instanceId);
    }

    public static HeavyTaskTracker removeTaskTracker(Long instanceId) {
        return INSTANCE_ID_2_TASK_TRACKER.remove(instanceId);
    }

    public static void atomicCreateTaskTracker(Long instanceId, Function<Long, HeavyTaskTracker> creator) {
        INSTANCE_ID_2_TASK_TRACKER.computeIfAbsent(instanceId, creator);
    }

    public static List<Long> getAllFrequentTaskTrackerKeys() {
        List<Long> keys = Lists.newLinkedList();
        INSTANCE_ID_2_TASK_TRACKER.forEach((key, tk) -> {
            if (tk instanceof FrequentTaskTracker) {
                keys.add(key);
            }
        });
        return keys;
    }

    public static int currentTaskTrackerSize(){
        return INSTANCE_ID_2_TASK_TRACKER.size();
    }
}
