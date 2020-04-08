package com.github.kfcfans.oms.worker.core.tracker.task;

import com.github.kfcfans.common.request.ServerScheduleJobReq;
import com.github.kfcfans.oms.worker.common.constants.TaskStatus;
import com.github.kfcfans.oms.worker.persistence.TaskDO;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.LongAdder;

/**
 * 处理秒级任务（FIX_RATE/FIX_DELAY）的TaskTracker
 *
 * @author tjq
 * @since 2020/4/8
 */
@Slf4j
public class FrequentTaskTracker extends TaskTracker {

    // 总运行次数
    private final LongAdder triggerTimes = new LongAdder();

    public FrequentTaskTracker(ServerScheduleJobReq req) {
        super(req);
    }


    /**
     * 新增任务并立即派发，用于 Frequent 任务
     * @param task 需要新增的任务
     * @return 是否成功
     */
    private boolean submitTaskAndDispatchImmediately(TaskDO task) {

        // 填充基础属性
        task.setJobId(instanceInfo.getJobId());
        task.setInstanceId(instanceInfo.getInstanceId());
        task.setStatus(TaskStatus.DISPATCH_SUCCESS_WORKER_UNCHECK.getValue());
        task.setFailedCnt(0);
        task.setLastModifiedTime(System.currentTimeMillis());
        task.setCreatedTime(System.currentTimeMillis());
        // 写入DB
        boolean success = taskPersistenceService.save(task);
        log.debug("[TaskTracker-{}] receive new immediately task: {}", instanceInfo.getInstanceId(), task);
        return success;
    }

    @Override
    protected void initTaskTracker(ServerScheduleJobReq req) {

    }
}
