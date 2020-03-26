package com.github.kfcfans.oms.worker.persistence;


import com.github.kfcfans.common.utils.CommonUtils;
import com.github.kfcfans.common.utils.SupplierPlus;
import com.github.kfcfans.oms.worker.common.constants.TaskConstant;
import com.github.kfcfans.oms.worker.common.constants.TaskStatus;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 任务持久化服务
 *
 * @author tjq
 * @since 2020/3/17
 */
@Slf4j
public class TaskPersistenceService {

    // 默认重试参数
    private static final int RETRY_TIMES = 3;
    private static final long RETRY_INTERVAL_MS = 100;

    private static volatile boolean initialized = false;
    public static TaskPersistenceService INSTANCE = new TaskPersistenceService();

    private TaskPersistenceService() {
    }

    private TaskDAO taskDAO = new TaskDAOImpl();

    public void init() throws Exception {
        if (initialized) {
            return;
        }
        taskDAO.initTable();
        initialized = true;
    }

    public boolean save(TaskDO task) {

        try {
            return execute(() -> taskDAO.save(task));
        }catch (Exception e) {
            log.error("[TaskPersistenceService] save task{} failed.",  task);
        }
        return false;
    }

    public boolean batchSave(List<TaskDO> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return true;
        }
        try {
            return execute(() -> taskDAO.batchSave(tasks));
        }catch (Exception e) {
            log.error("[TaskPersistenceService] batchSave tasks failed.", e);
        }
        return false;
    }

    /**
     * 获取 MapReduce 或 Broadcast 的最后一个任务
     */
    public Optional<TaskDO> getLastTask(String instanceId) {

        try {
            return execute(() -> {
                SimpleTaskQuery query = new SimpleTaskQuery();
                query.setInstanceId(instanceId);
                query.setTaskName(TaskConstant.LAST_TASK_NAME);
                List<TaskDO> taskDOS = taskDAO.simpleQuery(query);
                if (CollectionUtils.isEmpty(taskDOS)) {
                    return Optional.empty();
                }
                return Optional.of(taskDOS.get(0));
            });
        }catch (Exception e) {
            log.error("[TaskPersistenceService] get last task for instance(id={}) failed.", instanceId, e);
        }

        return Optional.empty();
    }

    public List<TaskDO> getAllTask(String instanceId) {
        try {
            return execute(() -> {
                SimpleTaskQuery query = new SimpleTaskQuery();
                query.setInstanceId(instanceId);
                return taskDAO.simpleQuery(query);
            });
        }catch (Exception e) {
            log.error("[TaskPersistenceService] getAllTask for instance(id={}) failed.", instanceId, e);
        }
        return Lists.newArrayList();
    }

    /**
     * 获取指定状态的Task
     */
    public List<TaskDO> getTaskByStatus(String instanceId, TaskStatus status, int limit) {
        try {
            return execute(() -> {
                SimpleTaskQuery query = new SimpleTaskQuery();
                query.setInstanceId(instanceId);
                query.setStatus(status.getValue());
                query.setLimit(limit);

                return taskDAO.simpleQuery(query);
            });
        }catch (Exception e) {
            log.error("[TaskPersistenceService] getTaskByStatus failed, params is instanceId={},status={}.", instanceId, status, e);
        }
        return Lists.newArrayList();
    }

    /**
     * 获取 TaskTracker 管理的子 task 状态统计信息
     * TaskStatus -> num
     */
    public Map<TaskStatus, Long> getTaskStatusStatistics(String instanceId) {
        try {
            return execute(() -> {
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
            });
        }catch (Exception e) {
            log.error("[TaskPersistenceService] getTaskStatusStatistics for instance(id={}) failed.", instanceId, e);
        }
        return Maps.newHashMap();
    }

    /**
     * 查询 taskId -> taskResult，reduce阶段或postProcess 阶段使用
     */
    public Map<String, String> getTaskId2ResultMap(String instanceId) {
        try {
            return execute(() -> taskDAO.queryTaskId2TaskResult(instanceId));
        }catch (Exception e) {
            log.error("[TaskPersistenceService] getTaskId2ResultMap for instance(id={}) failed.", instanceId, e);
        }
        return Maps.newHashMap();
    }

    /**
     * 查询任务状态（只查询 status，节约 I/O 资源）
     */
    public Optional<TaskStatus> getTaskStatus(String instanceId, String taskId) {

        try {
            return execute(() -> {
                SimpleTaskQuery query = genKeyQuery(instanceId, taskId);
                query.setQueryContent("STATUS");
                List<Map<String, Object>> rows = taskDAO.simpleQueryPlus(query);
                return Optional.of(TaskStatus.of((int) rows.get(0).get("STATUS")));
            });
        }catch (Exception e) {
            log.error("[TaskPersistenceService] getTaskStatus failed, instanceId={},taskId={}.", instanceId, taskId, e);
        }
        return Optional.empty();
    }

    /**
     * 查询任务失败数量（只查询 failed_cnt，节约 I/O 资源）
     */
    public Optional<Integer> getTaskFailedCnt(String instanceId, String taskId) {

        try {
            return execute(() -> {
                SimpleTaskQuery query = genKeyQuery(instanceId, taskId);
                query.setQueryContent("failed_cnt");
                List<Map<String, Object>> rows = taskDAO.simpleQueryPlus(query);
                // 查询成功不可能为空
                return Optional.of((Integer) rows.get(0).get("FAILED_CNT"));
            });
        }catch (Exception e) {
            log.error("[TaskPersistenceService] getTaskFailedCnt failed, instanceId={},taskId={}.", instanceId, taskId, e);
        }
        return Optional.empty();
    }

    /**
     * 更新 Task 的状态
     */
    public boolean updateTaskStatus(String instanceId, String taskId, TaskStatus status, String result) {
        try {
            return execute(() -> {
                TaskDO updateEntity = new TaskDO();
                updateEntity.setStatus(status.getValue());
                updateEntity.setResult(result);
                return taskDAO.simpleUpdate(genKeyQuery(instanceId, taskId), updateEntity);
            });
        }catch (Exception e) {
            log.error("[TaskPersistenceService] updateTaskStatus failed, instanceId={},taskId={},status={},result={}.",
                    instanceId, taskId, status, result, e);
        }
        return false;
    }

    public boolean updateRetryTask(String instanceId, String taskId, int failedCnt) {

        try {
            return execute(() -> {
                TaskDO updateEntity = new TaskDO();
                updateEntity.setStatus(TaskStatus.WAITING_DISPATCH.getValue());
                // 重新选取 worker 节点重试
                updateEntity.setAddress("");
                updateEntity.setFailedCnt(failedCnt);
                return taskDAO.simpleUpdate(genKeyQuery(instanceId, taskId), updateEntity);
            });
        }catch (Exception e) {
            log.error("[TaskPersistenceService] updateRetryTask failed, instanceId={},taskId={},failedCnt={}.", instanceId, taskId, failedCnt, e);
        }
        return false;
    }


    public boolean batchDelete(String instanceId, List<String> taskIds) {
        try {
            return execute(() -> taskDAO.batchDelete(instanceId, taskIds));
        }catch (Exception e) {
            log.error("[TaskPersistenceService] batchDelete failed, instanceId={},taskIds={}.", instanceId, taskIds, e);
        }
        return false;
    }

    public List<TaskDO> listAll() {
        try {
            return execute(() -> {
                SimpleTaskQuery query = new SimpleTaskQuery();
                query.setQueryCondition("1 = 1");
                return taskDAO.simpleQuery(query);
            });
        }catch (Exception e) {
            log.error("[TaskPersistenceService] listAll failed.", e);
        }
        return Collections.emptyList();
    }

    private static SimpleTaskQuery genKeyQuery(String instanceId, String taskId) {
        SimpleTaskQuery condition = new SimpleTaskQuery();
        condition.setInstanceId(instanceId);
        condition.setTaskId(taskId);
        return condition;
    }

    private static  <T> T execute(SupplierPlus<T> executor) throws Exception {
        return CommonUtils.executeWithRetry(executor, RETRY_TIMES, RETRY_INTERVAL_MS);
    }
}
