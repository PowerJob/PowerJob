package tech.powerjob.worker.core.tracker.manager;

import com.google.common.collect.Maps;
import tech.powerjob.common.model.TaskGroupQuota;
import tech.powerjob.worker.core.tracker.task.light.LightTaskTracker;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * @author Echo009
 * @since 2022/9/23
 */
public class LightTaskTrackerManager {

    public static final double OVERLOAD_FACTOR = 1.3d;

    private static final Map<Long, LightTaskTracker> INSTANCE_ID_2_TASK_TRACKER = Maps.newConcurrentMap();

    /**
     * Per-group index: groupName -> set of instanceIds belonging to that group.
     * Used for per-group capacity checking and heartbeat reporting.
     */
    private static final ConcurrentHashMap<String, Set<Long>> GROUP_2_INSTANCE_IDS = new ConcurrentHashMap<>();


    public static LightTaskTracker getTaskTracker(Long instanceId) {
        return INSTANCE_ID_2_TASK_TRACKER.get(instanceId);
    }

    public static void removeTaskTracker(Long instanceId) {
        // 忽略 IDE 警告，这个判断非常有用！不加这个判断会导致：如果创建 TT（先执行 computeIfAbsent 正在将TT添加到 HashMap） 时报错，TT 主动调用 destroy 销毁（从 HashMap移除该 TT）时死锁
        if (INSTANCE_ID_2_TASK_TRACKER.containsKey(instanceId)) {
            LightTaskTracker tracker = INSTANCE_ID_2_TASK_TRACKER.remove(instanceId);
            // Clean up group index using the group stored in the tracker
            if (tracker != null) {
                String group = tracker.getTaskGroup();
                if (group != null) {
                    GROUP_2_INSTANCE_IDS.compute(group, (k, set) -> {
                        if (set == null) return null;
                        set.remove(instanceId);
                        return set.isEmpty() ? null : set;
                    });
                }
            }
        }
    }

    /**
     * Create a task tracker with group tracking (legacy signature without group — uses "default").
     */
    public static void atomicCreateTaskTracker(Long instanceId, Function<Long, LightTaskTracker> creator) {
        atomicCreateTaskTracker(instanceId, TaskGroupQuota.DEFAULT_GROUP, creator);
    }

    /**
     * Create a task tracker and register it in the per-group index.
     */
    public static void atomicCreateTaskTracker(Long instanceId, String groupName, Function<Long, LightTaskTracker> creator) {
        LightTaskTracker tracker = INSTANCE_ID_2_TASK_TRACKER.computeIfAbsent(instanceId, creator);
        if (tracker != null && groupName != null) {
            // Use compute() for atomic add to group index
            GROUP_2_INSTANCE_IDS.compute(groupName, (k, set) -> {
                if (set == null) {
                    set = ConcurrentHashMap.newKeySet();
                }
                set.add(instanceId);
                return set;
            });
        }
    }

    public static int currentTaskTrackerSize() {
        return INSTANCE_ID_2_TASK_TRACKER.size();
    }

    /**
     * Get the number of running lightweight task trackers for a specific group.
     */
    public static int currentTaskTrackerSizeByGroup(String groupName) {
        Set<Long> ids = GROUP_2_INSTANCE_IDS.get(groupName);
        return ids == null ? 0 : ids.size();
    }

    /**
     * Clear all tracking state. Called during worker shutdown.
     */
    public static void clearAll() {
        INSTANCE_ID_2_TASK_TRACKER.clear();
        GROUP_2_INSTANCE_IDS.clear();
    }
}
