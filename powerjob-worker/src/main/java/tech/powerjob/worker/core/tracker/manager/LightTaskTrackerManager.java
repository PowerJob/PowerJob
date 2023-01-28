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

    public static void removeTaskTracker(Long instanceId) {
        // 忽略印度的 IDE 警告，这个判断非常有用！！！不加这个判断会导致：如果创建 TT（先执行 computeIfAbsent 正在将TT添加到 HashMap） 时报错，TT 主动调用 destroy 销毁（从 HashMap移除该 TT）时死锁
        if (INSTANCE_ID_2_TASK_TRACKER.containsKey(instanceId)) {
            INSTANCE_ID_2_TASK_TRACKER.remove(instanceId);
        }
    }

    public static void atomicCreateTaskTracker(Long instanceId, Function<Long, LightTaskTracker> creator) {
        INSTANCE_ID_2_TASK_TRACKER.computeIfAbsent(instanceId, creator);
    }

    public static int currentTaskTrackerSize(){
        return INSTANCE_ID_2_TASK_TRACKER.size();
    }

}
