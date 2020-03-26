package com.github.kfcfans.oms.worker.persistence;


import com.github.kfcfans.oms.worker.common.constants.TaskConstant;
import com.github.kfcfans.oms.worker.common.constants.TaskStatus;
import com.google.common.collect.Maps;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * 任务持久化服务
 *
 * @author tjq
 * @since 2020/3/17
 */
public class TaskPersistenceService {

    private static volatile boolean initialized = false;
    public static TaskPersistenceService INSTANCE = new TaskPersistenceService();

    private TaskPersistenceService() {
    }

    private TaskDAO taskDAO = new TaskDAOImpl();
    private static final int MAX_BATCH_SIZE = 50;

    public void init() throws Exception {
        if (initialized) {
            return;
        }
        taskDAO.initTable();
    }

    public boolean save(TaskDO task) {
        boolean success = taskDAO.save(task);
        if (!success) {
            try {
                Thread.sleep(100);
                success = taskDAO.save(task);
            }catch (Exception ignore) {
            }
        }
        return success;
    }

    public boolean batchSave(List<TaskDO> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return true;
        }
        return taskDAO.batchSave(tasks);
    }

    /**
     * 获取 MapReduce 或 Broadcast 的最后一个任务
     */
    public TaskDO getLastTask(String instanceId) {
        SimpleTaskQuery query = new SimpleTaskQuery();
        query.setInstanceId(instanceId);
        query.setTaskName(TaskConstant.LAST_TASK_NAME);
        List<TaskDO> taskDOS = taskDAO.simpleQuery(query);
        if (CollectionUtils.isEmpty(taskDOS)) {
            return null;
        }
        return taskDOS.get(0);
    }

    public List<TaskDO> getAllTask(String instanceId) {
        SimpleTaskQuery query = new SimpleTaskQuery();
        query.setInstanceId(instanceId);
        return taskDAO.simpleQuery(query);
    }

    /**
     * 获取指定状态的Task
     */
    public List<TaskDO> getTaskByStatus(String instanceId, TaskStatus status, int limit) {
        SimpleTaskQuery query = new SimpleTaskQuery();
        query.setInstanceId(instanceId);
        query.setStatus(status.getValue());
        query.setLimit(limit);

        return taskDAO.simpleQuery(query);
    }

    /**
     * 获取 TaskTracker 管理的子 task 状态统计信息
     * TaskStatus -> num
     */
    public Map<TaskStatus, Long> getTaskStatusStatistics(String instanceId) {
        SimpleTaskQuery query = new SimpleTaskQuery();
        query.setInstanceId(instanceId);
        query.setQueryContent("status, count(*) as num");
        query.setOtherCondition("GROUP BY status");
        List<Map<String, Object>> dbRES = taskDAO.simpleQueryPlus(query);

        Map<TaskStatus, Long> result = Maps.newHashMap();
        dbRES.forEach(row -> {
            // H2 数据库都是大写...
            int status = Integer.parseInt(String.valueOf(row.get("STATUS")));
            long num = Long.parseLong(String.valueOf(row.get("NUM")));
            result.put(TaskStatus.of(status), num);
        });
        return result;
    }

    /**
     * 查询 taskId -> taskResult，reduce阶段或postProcess 阶段使用
     */
    public Map<String, String> getTaskId2ResultMap(String instanceId) {
        return taskDAO.queryTaskId2TaskResult(instanceId);
    }

    /**
     * 查询任务状态（只查询 status，节约 I/O 资源）
     */
    public TaskStatus getTaskStatus(String instanceId, String taskId) {

        SimpleTaskQuery query = new SimpleTaskQuery();
        query.setInstanceId(instanceId);
        query.setTaskId(taskId);
        query.setQueryContent(" STATUS ");

        List<Map<String, Object>> rows = taskDAO.simpleQueryPlus(query);

        if (CollectionUtils.isEmpty(rows)) {
            return null;
        }

        return TaskStatus.of((int) rows.get(0).get("STATUS"));
    }

    /**
     * 更新 Task 的状态
     */
    public boolean updateTaskStatus(String instanceId, String taskId, TaskStatus status, String result) {
        SimpleTaskQuery condition = new SimpleTaskQuery();
        condition.setInstanceId(instanceId);
        condition.setTaskId(taskId);
        TaskDO updateEntity = new TaskDO();
        updateEntity.setStatus(status.getValue());
        updateEntity.setResult(result);
        return taskDAO.simpleUpdate(condition, updateEntity);
    }


    public int batchDelete(String instanceId, List<String> taskIds) {
        return taskDAO.batchDelete(instanceId, taskIds);
    }

    public List<TaskDO> listAll() {
        SimpleTaskQuery query = new SimpleTaskQuery();
        query.setQueryCondition("1 = 1");
        return taskDAO.simpleQuery(query);
    }
}
