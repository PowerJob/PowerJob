package tech.powerjob.worker.core.tracker.manager;

import com.google.common.collect.Maps;
import tech.powerjob.worker.core.tracker.task.light.LightTaskTracker;

import java.util.Map;
import java.util.function.Function;

/**
 * @author Echo009
 * @since 2022/9/23
 */
public class LightTaskTrackerManager {

    public static final double OVERLOAD_FACTOR = 1.3d;

    private static final Map<Long, LightTaskTracker> INSTANCE_ID_2_TASK_TRACKER = Maps.newConcurrentMap();


    public static LightTaskTracker getTaskTracker(Long instanceId) {
        return INSTANCE_ID_2_TASK_TRACKER.get(instanceId);
    }

    public static LightTaskTracker removeTaskTracker(Long instanceId) {
        return INSTANCE_ID_2_TASK_TRACKER.remove(instanceId);
    }

    public static void atomicCreateTaskTracker(Long instanceId, Function<Long, LightTaskTracker> creator) {
        INSTANCE_ID_2_TASK_TRACKER.computeIfAbsent(instanceId, creator);
    }

    public static int currentTaskTrackerSize(){
        return INSTANCE_ID_2_TASK_TRACKER.size();
    }

}
