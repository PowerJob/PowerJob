package com.github.kfcfans.oms.worker.core.tracker.processor;

import com.github.kfcfans.oms.worker.common.constants.TaskStatus;
import com.github.kfcfans.oms.worker.core.executor.ProcessorRunnable;
import com.github.kfcfans.oms.worker.persistence.TaskDO;
import com.github.kfcfans.oms.worker.persistence.TaskPersistenceService;
import com.github.kfcfans.oms.worker.pojo.request.TaskTrackerStartTaskReq;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池拒绝策略 -> 持久化到本地H2数据库
 * 出于内存占用考虑，线程池的阻塞队列容量不大，大量子任务涌入时需要持久化到本地数据库
 * 第一版先直接写H2，如果发现有性能问题再转变为 内存队列 + 批量写入 模式
 *
 * @author tjq
 * @since 2020/3/23
 */
@Slf4j
@AllArgsConstructor
public class RejectedProcessorHandler implements RejectedExecutionHandler {

    private final TaskPersistenceService taskPersistenceService;

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {

        ProcessorRunnable processorRunnable = (ProcessorRunnable) r;
        TaskTrackerStartTaskReq startTaskReq = processorRunnable.getRequest();

        TaskDO newTask = new TaskDO();
        BeanUtils.copyProperties(startTaskReq, newTask);
        newTask.setTaskContent(startTaskReq.getSubTaskContent());
        newTask.setAddress(startTaskReq.getTaskTrackerAddress());
        newTask.setStatus(TaskStatus.RECEIVE_SUCCESS.getValue());
        newTask.setFailedCnt(0);
        newTask.setCreatedTime(System.currentTimeMillis());
        newTask.setLastModifiedTime(System.currentTimeMillis());

        boolean save = taskPersistenceService.save(newTask);
        if (save) {
            log.debug("[RejectedProcessorHandler] persistent task({}) succeed.", newTask);
        }else {
            log.warn("[RejectedProcessorHandler] persistent task({}) failed.", newTask);
        }
    }
}
