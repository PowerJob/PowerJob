package tech.powerjob.worker.persistence;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * 持久化器管理
 *
 * @author tjq
 * @since 2024/2/25
 */
public class PersistenceServiceManager {

    private static final Map<Long, TaskPersistenceService> INSTANCE_ID_2_TASK_PERSISTENCE_SERVICE = Maps.newConcurrentMap();

    public static void register(Long instanceId, TaskPersistenceService taskPersistenceService) {
        INSTANCE_ID_2_TASK_PERSISTENCE_SERVICE.put(instanceId, taskPersistenceService);
    }

    public static void unregister(Long instanceId) {
        INSTANCE_ID_2_TASK_PERSISTENCE_SERVICE.remove(instanceId);
    }

    public static TaskPersistenceService fetchTaskPersistenceService(Long instanceId) {
        return INSTANCE_ID_2_TASK_PERSISTENCE_SERVICE.get(instanceId);
    }
}
