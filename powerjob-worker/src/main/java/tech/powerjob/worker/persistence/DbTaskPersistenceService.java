package tech.powerjob.worker.persistence;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import tech.powerjob.common.RemoteConstant;
import tech.powerjob.common.utils.CollectionUtils;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.common.utils.SupplierPlus;
import tech.powerjob.worker.common.constants.StoreStrategy;
import tech.powerjob.worker.common.constants.TaskConstant;
import tech.powerjob.worker.common.constants.TaskStatus;
import tech.powerjob.worker.core.processor.TaskResult;
import tech.powerjob.worker.persistence.db.ConnectionFactory;
import tech.powerjob.worker.persistence.db.SimpleTaskQuery;
import tech.powerjob.worker.persistence.db.TaskDAO;
import tech.powerjob.worker.persistence.db.TaskDAOImpl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 基于内置数据库的任务持久化服务
 *
 * @author tjq
 * @since 2024/2/23
 */
@Slf4j
public class DbTaskPersistenceService implements TaskPersistenceService {

    private final StoreStrategy strategy;

    /**
     * 默认重试次数
     */
    private static final int RETRY_TIMES = 3;

    private static final long RETRY_INTERVAL_MS = 100;

    /**
     * 慢查询定义：200ms
     */
    private static final long SLOW_QUERY_RT_THRESHOLD = 200;

    private TaskDAO taskDAO;

    public DbTaskPersistenceService(StoreStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public void init() throws Exception {

        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.initDatasource(strategy);

        taskDAO = new TaskDAOImpl(connectionFactory);
        taskDAO.initTable();
    }

    @Override
    public boolean batchSave(List<TaskDO> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return true;
        }
        try {
            return execute(() -> taskDAO.batchSave(tasks), cost -> log.warn("[TaskPersistenceService] [Slow] [{}] batchSave cost {}ms", tasks.get(0).getInstanceId(), cost));
        }catch (Exception e) {
            log.error("[TaskPersistenceService] batchSave tasks({}) failed.", tasks, e);
        }
        return false;
    }

    /**
     * 依靠主键更新 Task（不涉及 result 的，都可以用该方法更新）
     */
    @Override
    public boolean updateTask(Long instanceId, String taskId, TaskDO updateEntity) {
        try {
            updateEntity.setLastModifiedTime(System.currentTimeMillis());
            SimpleTaskQuery query = genKeyQuery(instanceId, taskId);
            return execute(() -> taskDAO.simpleUpdate(query, updateEntity), cost -> log.warn("[TaskPersistenceService] [Slow] [{}] updateTask(taskId={}) cost {}ms", instanceId, taskId, cost));
        }catch (Exception e) {
            log.error("[TaskPersistenceService] updateTask failed.", e);
        }
        return false;
    }

    /**
     * 更新任务状态
     */
    @Override
    public boolean updateTaskStatus(Long instanceId, String taskId, int status, long lastReportTime, String result) {
        try {
            return execute(() -> taskDAO.updateTaskStatus(instanceId, taskId, status, lastReportTime, result), cost -> log.warn("[TaskPersistenceService] [Slow] [{}] updateTaskStatus(taskId={}) cost {}ms", instanceId, taskId, cost));
        }catch (Exception e) {
            log.error("[TaskPersistenceService] updateTaskStatus failed.", e);
        }
        return false;
    }

    /**
     * 更新被派发到已经失联的 ProcessorTracker 的任务，重新执行
     * update task_info
     * set address = 'N/A', status = 0
     * where address in () and status not in (5,6) and instance_id = 277
     */
    @Override
    public boolean updateLostTasks(Long instanceId, List<String> addressList, boolean retry) {

        TaskDO updateEntity = new TaskDO();
        updateEntity.setLastModifiedTime(System.currentTimeMillis());
        if (retry) {
            updateEntity.setAddress(RemoteConstant.EMPTY_ADDRESS);
            updateEntity.setStatus(TaskStatus.WAITING_DISPATCH.getValue());
        }else {
            updateEntity.setStatus(TaskStatus.WORKER_PROCESS_FAILED.getValue());
            updateEntity.setResult("maybe worker down");
        }

        SimpleTaskQuery query = new SimpleTaskQuery();
        query.setInstanceId(instanceId);
        String queryConditionFormat = "address in %s and status not in (%d, %d)";
        String queryCondition = String.format(queryConditionFormat, CommonUtils.getInStringCondition(addressList), TaskStatus.WORKER_PROCESS_FAILED.getValue(), TaskStatus.WORKER_PROCESS_SUCCESS.getValue());
        query.setQueryCondition(queryCondition);
        log.debug("[TaskPersistenceService] updateLostTasks-QUERY-SQL: {}", query.getQueryCondition());

        try {
            return execute(() -> taskDAO.simpleUpdate(query, updateEntity), cost -> log.warn("[TaskPersistenceService] [Slow] [{}] updateLostTasks cost {}ms", instanceId, cost));
        }catch (Exception e) {
            log.error("[TaskPersistenceService] updateLostTasks failed.", e);
        }
        return false;
    }

    /**
     * 获取 MapReduce 或 Broadcast 的最后一个任务
     */
    @Override
    public Optional<TaskDO> getLastTask(Long instanceId, Long subInstanceId) {

        try {
            SimpleTaskQuery query = new SimpleTaskQuery();
            query.setInstanceId(instanceId);
            query.setSubInstanceId(subInstanceId);
            query.setTaskName(TaskConstant.LAST_TASK_NAME);
            return execute(() -> {
                List<TaskDO> taskDOS = taskDAO.simpleQuery(query);
                if (CollectionUtils.isEmpty(taskDOS)) {
                    return Optional.empty();
                }
                return Optional.of(taskDOS.get(0));
            }, cost -> log.warn("[TaskPersistenceService] [Slow] [{}.{}] getLastTask cost {}ms", instanceId, subInstanceId, cost));
        }catch (Exception e) {
            log.error("[TaskPersistenceService] get last task for instance(id={}) failed.", instanceId, e);
        }

        return Optional.empty();
    }

    /**
     * 获取某个 ProcessorTracker 未完成的任务
     * @param instanceId instanceId
     * @param address address
     * @return result
     */
    @Override
    public List<TaskDO> getAllUnFinishedTaskByAddress(Long instanceId, String address) {
        try {
            String condition = String.format("status not in (%d, %d)", TaskStatus.WORKER_PROCESS_SUCCESS.getValue(), TaskStatus.WORKER_PROCESS_FAILED.getValue());

            SimpleTaskQuery query = new SimpleTaskQuery();
            query.setInstanceId(instanceId);
            query.setAddress(address);
            query.setQueryCondition(condition);

            return execute(() -> taskDAO.simpleQuery(query) , cost -> log.warn("[TaskPersistenceService] [Slow] [{}] getAllUnFinishedTaskByAddress({}) cost {}ms", instanceId, address, cost));
        }catch (Exception e) {
            log.error("[TaskPersistenceService] getAllTaskByAddress for instance(id={}) failed.", instanceId, e);
        }
        return Lists.newArrayList();
    }

    /**
     * 获取指定状态的Task
     */
    @Override
    public List<TaskDO> getTaskByStatus(Long instanceId, TaskStatus status, int limit) {
        try {
            SimpleTaskQuery query = new SimpleTaskQuery();
            query.setInstanceId(instanceId);
            query.setStatus(status.getValue());
            query.setLimit(limit);
            return execute(() -> taskDAO.simpleQuery(query), cost -> log.warn("[TaskPersistenceService] [Slow] [{}] getTaskByStatus({}) cost {}ms", instanceId, status, cost));
        }catch (Exception e) {
            log.error("[TaskPersistenceService] getTaskByStatus failed, params is instanceId={},status={}.", instanceId, status, e);
        }
        return Lists.newArrayList();
    }

    /**
     * 获取 TaskTracker 管理的子 task 状态统计信息
     * TaskStatus -> num
     */
    @Override
    public Map<TaskStatus, Long> getTaskStatusStatistics(Long instanceId, Long subInstanceId) {
        try {

            SimpleTaskQuery query = new SimpleTaskQuery();
            query.setInstanceId(instanceId);
            query.setSubInstanceId(subInstanceId);
            query.setQueryContent("status, count(*) as num");
            query.setOtherCondition("GROUP BY status");

            return execute(() -> {
                List<Map<String, Object>> dbRES = taskDAO.simpleQueryPlus(query);
                Map<TaskStatus, Long> result = Maps.newHashMap();
                dbRES.forEach(row -> {
                    // H2 数据库都是大写...
                    int status = Integer.parseInt(String.valueOf(row.get("status")));
                    long num = Long.parseLong(String.valueOf(row.get("num")));
                    result.put(TaskStatus.of(status), num);
                });
                return result;
            }, cost -> log.warn("[TaskPersistenceService] [Slow] [{}.{}] getTaskStatusStatistics cost {}ms", instanceId, subInstanceId, cost));
        }catch (Exception e) {
            log.error("[TaskPersistenceService] getTaskStatusStatistics for instance(id={}) failed.", instanceId, e);
        }
        return Maps.newHashMap();
    }

    /**
     * 查询所有Task执行结果，reduce阶段 或 postProcess阶段 使用
     */
    @Override
    public List<TaskResult> getAllTaskResult(Long instanceId, Long subInstanceId) {
        try {
            return execute(() -> taskDAO.getAllTaskResult(instanceId, subInstanceId), cost -> log.warn("[TaskPersistenceService] [Slow] [{}.{}] getAllTaskResult cost {}ms", instanceId, subInstanceId, cost));
        }catch (Exception e) {
            log.error("[TaskPersistenceService] getTaskId2ResultMap for instance(id={}) failed.", instanceId, e);
        }
        return Lists.newLinkedList();
    }

    @Override
    public List<TaskDO> getTaskByQuery(Long instanceId, String customQuery) {
        SimpleTaskQuery simpleTaskQuery = new SimpleTaskQuery();
        simpleTaskQuery.setInstanceId(instanceId);
        simpleTaskQuery.setFullCustomQueryCondition(customQuery);
        simpleTaskQuery.setReadOnly(true);
        try {
            return execute(() -> taskDAO.simpleQuery(simpleTaskQuery), cost -> log.warn("[TaskPersistenceService] [Slow] [{}] getTaskByQuery cost {}ms", instanceId, cost));
        }catch (Exception e) {
            log.error("[TaskPersistenceService] getTaskByQuery for instance(id={}) failed.", instanceId, e);
        }
        return Lists.newLinkedList();
    }

    /**
     * 根据主键查询 Task
     */
    @Override
    public Optional<TaskDO> getTask(Long instanceId, String taskId) {
        try {
            SimpleTaskQuery query = genKeyQuery(instanceId, taskId);
            return execute(() -> {
                List<TaskDO> res = taskDAO.simpleQuery(query);
                if (CollectionUtils.isEmpty(res)) {
                    return Optional.empty();
                }
                return Optional.of(res.get(0));
            }, cost -> log.warn("[TaskPersistenceService] [Slow] [{}] getTask(taskId={}) cost {}ms", instanceId, taskId, cost));
        }catch (Exception e) {
            log.error("[TaskPersistenceService] getTask failed, instanceId={},taskId={}.", instanceId, taskId, e);
        }
        return Optional.empty();
    }

    @Override
    public boolean deleteAllTasks(Long instanceId) {
        try {
            SimpleTaskQuery condition = new SimpleTaskQuery();
            condition.setInstanceId(instanceId);
            return execute(() -> taskDAO.simpleDelete(condition), cost -> log.warn("[TaskPersistenceService] [Slow] [{}] deleteAllTasks cost {}ms", instanceId, cost));
        }catch (Exception e) {
            log.error("[TaskPersistenceService] deleteAllTasks failed, instanceId={}.", instanceId, e);
        }
        return false;
    }

    @Override
    public boolean deleteAllSubInstanceTasks(Long instanceId, Long subInstanceId) {
        try {
            SimpleTaskQuery condition = new SimpleTaskQuery();
            condition.setInstanceId(instanceId);
            condition.setSubInstanceId(subInstanceId);
            return execute(() -> taskDAO.simpleDelete(condition), cost -> log.warn("[TaskPersistenceService] [Slow] [{}.{}] deleteAllSubInstanceTasks cost {}ms", instanceId, subInstanceId, cost));
        }catch (Exception e) {
            log.error("[TaskPersistenceService] deleteAllTasks failed, instanceId={}.", instanceId, e);
        }
        return false;
    }

    @Override
    public boolean deleteTasksByTaskIds(Long instanceId, Collection<String> taskId) {
        try {
            SimpleTaskQuery condition = new SimpleTaskQuery();
            condition.setInstanceId(instanceId);
            condition.setTaskIds(taskId);
            return execute(() -> taskDAO.simpleDelete(condition), cost -> log.warn("[TaskPersistenceService] [Slow] [{}] deleteTasksByTaskIds cost {}ms", instanceId, cost));
        }catch (Exception e) {
            log.error("[TaskPersistenceService] deleteTasksByTaskIds failed, instanceId={}.", instanceId, e);
        }
        return false;
    }

    private static SimpleTaskQuery genKeyQuery(Long instanceId, String taskId) {
        SimpleTaskQuery condition = new SimpleTaskQuery();
        condition.setInstanceId(instanceId);
        condition.setTaskId(taskId);
        return condition;
    }

    private static  <T> T execute(SupplierPlus<T> executor, Consumer<Long> slowQueryLogger) throws Exception {
        long s = System.currentTimeMillis();
        try {
            return CommonUtils.executeWithRetry(executor, RETRY_TIMES, RETRY_INTERVAL_MS);
        } finally {
            long cost = System.currentTimeMillis() - s;
            if (cost > SLOW_QUERY_RT_THRESHOLD) {
                slowQueryLogger.accept(cost);
            }
        }
    }
}
