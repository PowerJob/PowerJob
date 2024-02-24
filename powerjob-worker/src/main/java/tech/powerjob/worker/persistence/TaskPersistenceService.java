package tech.powerjob.worker.persistence;


import tech.powerjob.worker.common.constants.TaskStatus;
import tech.powerjob.worker.core.processor.TaskResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 任务持久化服务
 *
 * @author tjq
 * @since 2020/3/17
 */
public interface TaskPersistenceService {

    void init() throws Exception;

    boolean batchSave(List<TaskDO> tasks);

    boolean updateTask(Long instanceId, String taskId, TaskDO updateEntity);

    boolean updateTaskStatus(Long instanceId, String taskId, int status, long lastReportTime, String result);

    boolean updateLostTasks(Long instanceId, List<String> addressList, boolean retry);

    Optional<TaskDO> getLastTask(Long instanceId, Long subInstanceId);

    List<TaskDO> getAllUnFinishedTaskByAddress(Long instanceId, String address);

    List<TaskDO> getTaskByStatus(Long instanceId, TaskStatus status, int limit);

    List<TaskDO> getTaskByQuery(Long instanceId, String customQuery);

    Map<TaskStatus, Long> getTaskStatusStatistics(Long instanceId, Long subInstanceId);

    List<TaskResult> getAllTaskResult(Long instanceId, Long subInstanceId);

    Optional<TaskDO> getTask(Long instanceId, String taskId);

    boolean deleteAllTasks(Long instanceId);

    boolean deleteAllSubInstanceTasks(Long instanceId, Long subInstanceId);

    boolean deleteTasksByTaskIds(Long instanceId, Collection<String> taskId);
}
