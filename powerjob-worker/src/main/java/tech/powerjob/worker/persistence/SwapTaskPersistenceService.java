package tech.powerjob.worker.persistence;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import tech.powerjob.common.PowerJobDKey;
import tech.powerjob.common.enhance.SafeRunnable;
import tech.powerjob.common.enums.ExecuteType;
import tech.powerjob.common.utils.CollectionUtils;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.common.utils.MapUtils;
import tech.powerjob.worker.common.constants.TaskStatus;
import tech.powerjob.worker.core.processor.TaskResult;
import tech.powerjob.worker.persistence.fs.ExternalTaskPersistenceService;
import tech.powerjob.worker.persistence.fs.impl.ExternalTaskFileSystemPersistenceService;
import tech.powerjob.worker.pojo.model.InstanceInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

/**
 * SWAP：换入换出降低运行时开销
 *
 * @author tjq
 * @since 2024/2/23
 */
@Slf4j
public class SwapTaskPersistenceService implements TaskPersistenceService {

    private final Long instanceId;
    private final long maxActiveTaskNum;
    private final long scheduleRateMs;
    /**
     * 数据库记录数量，不要求完全精确，仅用于控制存哪里，有一定容忍度
     */
    private final LongAdder dbRecordNum = new LongAdder();
    /**
     * 外部存储的任务数量，必须精确，否则会导致任务卡住
     */
    private final LongAdder externalPendingRecordNum = new LongAdder();
    private final LongAdder externalSucceedRecordNum = new LongAdder();
    private final LongAdder externalFailedRecordNum = new LongAdder();

    private final boolean needResult;
    private final boolean canUseSwap;
    private final TaskPersistenceService dbTaskPersistenceService;

    private boolean swapEnabled;
    private volatile boolean finished = false;
    private ExternalTaskPersistenceService externalTaskPersistenceService;

    /**
     * 保险措施，当外部数据长时间空时，至少能顺利结束任务，而不是一直卡着
     */
    private long lastExternalPendingEmptyTime = -1;
    private static final long MAX_EXTERNAL_PENDING_WAIT_TIME = 600000;

    /**
     * 默认最大活跃任务数量
     */
    private static final long DEFAULT_RUNTIME_MAX_ACTIVE_TASK_NUM = 100000;

    /**
     * 默认工作频率
     */
    private static final long DEFAULT_SCHEDULE_TIME = 60000;

    public SwapTaskPersistenceService(InstanceInfo instanceInfo, TaskPersistenceService dbTaskPersistenceService) {
        this.instanceId = instanceInfo.getInstanceId();
        this.needResult = ExecuteType.MAP_REDUCE.name().equalsIgnoreCase(instanceInfo.getExecuteType());
        this.canUseSwap = ExecuteType.MAP.name().equalsIgnoreCase(instanceInfo.getExecuteType()) || ExecuteType.MAP_REDUCE.name().equalsIgnoreCase(instanceInfo.getExecuteType());
        this.dbTaskPersistenceService = dbTaskPersistenceService;
        this.maxActiveTaskNum = Long.parseLong(System.getProperty(PowerJobDKey.WORKER_RUNTIME_SWAP_MAX_ACTIVE_TASK_NUM, String.valueOf(DEFAULT_RUNTIME_MAX_ACTIVE_TASK_NUM)));
        this.scheduleRateMs = Long.parseLong(System.getProperty(PowerJobDKey.WORKER_RUNTIME_SWAP_TASK_SCHEDULE_INTERVAL_MS, String.valueOf(DEFAULT_SCHEDULE_TIME)));
        PersistenceServiceManager.register(this.instanceId, this);
        log.info("[SwapTaskPersistenceService-{}] initialized SwapTaskPersistenceService, canUseSwap: {}, needResult: {}, maxActiveTaskNum: {}, scheduleRateMs: {}", instanceId, canUseSwap, needResult, maxActiveTaskNum, scheduleRateMs);
    }

    @Override
    public void init() throws Exception {
    }

    @Override
    public boolean updateTask(Long instanceId, String taskId, TaskDO updateEntity) {
        return dbTaskPersistenceService.updateTask(instanceId, taskId, updateEntity);
    }

    @Override
    public boolean updateTaskStatus(Long instanceId, String taskId, int status, long lastReportTime, String result) {
        return dbTaskPersistenceService.updateTaskStatus(instanceId, taskId, status, lastReportTime, result);
    }

    @Override
    public boolean updateLostTasks(Long instanceId, List<String> addressList, boolean retry) {
        return dbTaskPersistenceService.updateLostTasks(instanceId, addressList, retry);
    }

    @Override
    public Optional<TaskDO> getLastTask(Long instanceId, Long subInstanceId) {
        return dbTaskPersistenceService.getLastTask(instanceId, subInstanceId);
    }

    @Override
    public List<TaskDO> getAllUnFinishedTaskByAddress(Long instanceId, String address) {
        return dbTaskPersistenceService.getAllUnFinishedTaskByAddress(instanceId, address);
    }

    @Override
    public List<TaskDO> getTaskByStatus(Long instanceId, TaskStatus status, int limit) {
        return dbTaskPersistenceService.getTaskByStatus(instanceId, status, limit);
    }

    @Override
    public List<TaskDO> getTaskByQuery(Long instanceId, String customQuery) {
        return dbTaskPersistenceService.getTaskByQuery(instanceId, customQuery);
    }

    @Override
    public Optional<TaskDO> getTask(Long instanceId, String taskId) {
        return dbTaskPersistenceService.getTask(instanceId, taskId);
    }

    @Override
    public boolean deleteAllSubInstanceTasks(Long instanceId, Long subInstanceId) {
        return dbTaskPersistenceService.deleteAllSubInstanceTasks(instanceId, subInstanceId);
    }

    @Override
    public boolean deleteTasksByTaskIds(Long instanceId, Collection<String> taskId) {
        return dbTaskPersistenceService.deleteTasksByTaskIds(instanceId, taskId);
    }

    /* 重写区 */

    @Override
    public boolean batchSave(List<TaskDO> tasks) {

        long dbNum = dbRecordNum.sum();

        if (canUseSwap && dbNum > maxActiveTaskNum) {

            // 上层保证启用 SWAP 的任务，batchSave 的都是等待调度的任务，不会参与真正的运行
            boolean persistPendingTaskRes = getExternalTaskPersistenceService().persistPendingTask(tasks);

            // 仅成功情况累加（按严格模式累加），防止出现任务无法停止的问题。文件系统实际应该比较稳定，此处出错概率不高
            if (persistPendingTaskRes) {
                externalPendingRecordNum.add(tasks.size());
            }

            log.debug("[SwapTaskPersistenceService-{}] too many tasks at runtime(dbRecordNum: {}), SWAP enabled, persistence result: {}, externalPendingRecordNum: {}", instanceId, dbNum, persistPendingTaskRes, externalPendingRecordNum);
            return persistPendingTaskRes;
        } else {
            return persistTask2Db(tasks);
        }
    }

    @Override
    public boolean deleteAllTasks(Long instanceId) {
        finished = true;
        CommonUtils.executeIgnoreException(() -> {
            if (swapEnabled) {
                externalTaskPersistenceService.close();
            }
        });
        PersistenceServiceManager.unregister(instanceId);
        return dbTaskPersistenceService.deleteAllTasks(instanceId);
    }

    @Override
    public Map<TaskStatus, Long> getTaskStatusStatistics(Long instanceId, Long subInstanceId) {
        Map<TaskStatus, Long> taskStatusStatistics = dbTaskPersistenceService.getTaskStatusStatistics(instanceId, subInstanceId);
        if (!swapEnabled) {
            return taskStatusStatistics;
        }

        long waitingNum = MapUtils.getLongValue(taskStatusStatistics, TaskStatus.WAITING_DISPATCH) + externalPendingRecordNum.sum();
        long succeedNum = MapUtils.getLongValue(taskStatusStatistics, TaskStatus.WORKER_PROCESS_SUCCESS) + externalSucceedRecordNum.sum();
        long failedNum = MapUtils.getLongValue(taskStatusStatistics, TaskStatus.WORKER_PROCESS_FAILED) + externalFailedRecordNum.sum();

        taskStatusStatistics.put(TaskStatus.WAITING_DISPATCH, waitingNum);
        taskStatusStatistics.put(TaskStatus.WORKER_PROCESS_SUCCESS, succeedNum);
        taskStatusStatistics.put(TaskStatus.WORKER_PROCESS_FAILED, failedNum);

        return taskStatusStatistics;
    }

    @Override
    public List<TaskResult> getAllTaskResult(Long instanceId, Long subInstanceId) {

        List<TaskResult> dbTaskResult = dbTaskPersistenceService.getAllTaskResult(instanceId, subInstanceId);
        if (!swapEnabled) {
            return dbTaskResult;
        }

        List<TaskResult> allTaskResult = Lists.newLinkedList(dbTaskResult);
        while (true) {
            List<TaskDO> externalTask = externalTaskPersistenceService.readFinishedTask();
            if (CollectionUtils.isEmpty(externalTask)) {
                break;
            }
            externalTask.forEach(t -> {
                TaskResult taskResult = new TaskResult();
                taskResult.setTaskId(t.getTaskId());
                taskResult.setSuccess(TaskStatus.WORKER_PROCESS_SUCCESS.getValue() == t.getStatus());
                taskResult.setResult(t.getResult());

                allTaskResult.add(taskResult);
            });
        }

        return allTaskResult;

        // TODO: 后续支持 stream 流式 reduce
    }

    private class YuGong extends SafeRunnable {

        @Override
        protected void run0() {
            while (true) {

                if (finished) {
                    return;
                }

                CommonUtils.easySleep(scheduleRateMs);

                // 顺序很关键，先移出才有空间移入
                moveOutFinishedTask();
                moveInPendingTask();
            }
        }

        private void moveInPendingTask() {

            while (true) {

                // 外部存储无数据，无需扫描
                if (externalPendingRecordNum.sum() <= 0) {
                    lastExternalPendingEmptyTime = -1;
                    if (externalPendingRecordNum.sum() < 0) {
                        log.warn("[SwapTaskPersistenceService-{}] externalPendingRecordNum({}) < 0, maybe there's a bug!", instanceId, externalPendingRecordNum);
                    }
                    return;
                }

                // 到达 DB 最大数量后跳出扫描
                if (dbRecordNum.sum() > maxActiveTaskNum) {
                    // DB为最大数量时，说明此时任务依然满载，不需要进行空超时统计
                    lastExternalPendingEmptyTime = -1;
                    return;
                }

                List<TaskDO> taskDOS = getExternalTaskPersistenceService().readPendingTask();

                // 队列空则跳出循环，等待下一次扫描
                if (CollectionUtils.isEmpty(taskDOS)) {

                    // 走到此处，会满足 DB有可用空间，当文件一直空数据。如果这个过程长期维持，则说明某些地方产生了异常导致判定失准，需要及时止损
                    if (lastExternalPendingEmptyTime < 0) {
                        lastExternalPendingEmptyTime = System.currentTimeMillis();
                    }

                    // 超时机制，处理：DB 存在可导入空间但长期无法拉到数据，同时 externalPendingRecordNum 一直非0导致任务无法判定结束的情况
                    long offset = System.currentTimeMillis() - lastExternalPendingEmptyTime;
                    if (offset > MAX_EXTERNAL_PENDING_WAIT_TIME) {
                        log.warn("[SwapTaskPersistenceService-{}] [moveInPendingTask] Unable to get tasks from external files for a long time, unexpected things may have happened(lastExternalPendingEmptyTime: {}, offsetFromNow: {}). System will reset externalPendingRecordNum so that the task can end(before reset externalPendingRecordNum: {}).", instanceId, lastExternalPendingEmptyTime, offset, externalPendingRecordNum);
                        externalPendingRecordNum.reset();
                        return;
                    }

                    log.debug("[SwapTaskPersistenceService-{}] [moveInPendingTask] readPendingTask from external is empty, finished this loop!", instanceId);
                    return;
                }

                // 一旦读取到数据就重置计时器
                lastExternalPendingEmptyTime = -1;

                // 一旦读取，无论结果如何都直接减数量，无论后续结果如何
                externalPendingRecordNum.add(-taskDOS.size());

                boolean persistTask2Db = persistTask2Db(taskDOS);
                log.debug("[SwapTaskPersistenceService-{}] [moveInPendingTask] readPendingTask size: {}, persistResult: {}, currentDbRecordNum: {}, remainExternalPendingRecordNum: {}", instanceId, taskDOS.size(), persistTask2Db, dbRecordNum, externalPendingRecordNum);

                // 持久化失败的情况，及时跳出本次循环，防止损失扩大，等待下次扫描
                if (!persistTask2Db) {
                    log.error("[SwapTaskPersistenceService-{}] [moveInPendingTask] moveIn task failed, these tasks are lost: {}", instanceId, taskDOS);
                    return;
                }
            }
        }

        private void moveOutFinishedTask() {

            while (true) {

                // 一旦启动 SWAP，需要移出更多的数据来灌入
                long maxRemainNum = maxActiveTaskNum / 2;
                if (dbRecordNum.sum() <= maxRemainNum) {
                    return;
                }

                List<TaskDO> succeedTasks = dbTaskPersistenceService.getTaskByStatus(instanceId, TaskStatus.WORKER_PROCESS_SUCCESS, 100);
                if (!CollectionUtils.isEmpty(succeedTasks)) {
                    moveOutDetailFinishedTask(succeedTasks, true);
                    // 优先搬运成功数据，100% 已固化（失败任务可能还夜长梦多）
                    continue;
                }

                List<TaskDO> failedTask = dbTaskPersistenceService.getTaskByStatus(instanceId, TaskStatus.WORKER_PROCESS_FAILED, 100);

                // 还没有已完成任务产生 or 移完了，先整体 finished 跳出循环，等待下个调度周期
                if (CollectionUtils.isEmpty(failedTask)) {
                    return;
                }

                moveOutDetailFinishedTask(failedTask, false);
            }
        }

        private void moveOutDetailFinishedTask(List<TaskDO> tasks, boolean success) {

            String logKey = String.format("[SwapTaskPersistenceService-%d] [moveOut%sTask] ", instanceId, success ? "Success" : "Failed");

            boolean persistFinishedTask2ExternalResult = getExternalTaskPersistenceService().persistFinishedTask(tasks);

            if (!persistFinishedTask2ExternalResult) {
                log.warn("{} persistFinishedTask to external failed, skip this stage!", logKey);
            }

            LongAdder externalRecord = success ? externalSucceedRecordNum : externalFailedRecordNum;

            // 持久化成功，直接记录数量，无论 DB 是否删除，外部数据已存在，100% 会被并入统计
            int moveOutNum = tasks.size();
            externalRecord.add(moveOutNum);

            List<String> deleteTaskIds = tasks.stream().map(TaskDO::getTaskId).collect(Collectors.toList());
            boolean deleteTasksByTaskIdsResult = dbTaskPersistenceService.deleteTasksByTaskIds(instanceId, deleteTaskIds);

            if (deleteTasksByTaskIdsResult) {
                dbRecordNum.add(-moveOutNum);
                log.debug("{} move task to external successfully(movedNum: {}, currentExternalSucceedNum: {}, currentExternalFailedNum: {}, currentDbRecordNum: {})", logKey, moveOutNum, externalSucceedRecordNum, externalFailedRecordNum, dbRecordNum);
            } else {
                // DB 删除失败影响不大，reduce 重复而已
                log.warn("{} persistFinishedTask to external successfully but delete in runtime failed(movedNum: {}, currentExternalSucceedNum: {}, currentExternalFailedNum: {}, currentDbRecordNum: {}), these taskIds may have duplicate results in reduce stage: {}", logKey, moveOutNum, externalSucceedRecordNum, externalFailedRecordNum, dbRecordNum, deleteTaskIds);
            }
        }
    }

    private boolean persistTask2Db(List<TaskDO> taskDOS) {
        dbRecordNum.add(taskDOS.size());
        return dbTaskPersistenceService.batchSave(taskDOS);
    }

    private ExternalTaskPersistenceService getExternalTaskPersistenceService() {
        if (externalTaskPersistenceService != null) {
            return externalTaskPersistenceService;
        }
        synchronized (this) {
            if (externalTaskPersistenceService != null) {
                return externalTaskPersistenceService;
            }

            // 初始化 SWAP 相关内容
            this.swapEnabled = true;
            this.externalTaskPersistenceService = new ExternalTaskFileSystemPersistenceService(instanceId, needResult);
            new Thread(new YuGong(), "PJ-YuGong-" + instanceId).start();

            return externalTaskPersistenceService;
        }
    }
}
