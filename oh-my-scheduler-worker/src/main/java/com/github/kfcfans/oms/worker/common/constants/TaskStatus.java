package com.github.kfcfans.oms.worker.common.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务状态，task_info 表中 status 字段的枚举值
 *
 * @author tjq
 * @since 2020/3/17
 */
@Getter
@AllArgsConstructor
public enum TaskStatus {

    /* ******************* TaskTracker 专用 ******************* */
    WAITING_DISPATCH(1, "等待调度器调度"),
    DISPATCH_SUCCESS_WORKER_UNCHECK(2, "调度成功（但不保证worker收到）"),
    WORKER_RECEIVED(3, "worker接收成功，但未开始执行"),
    WORKER_PROCESSING(4, "worker正在执行"),
    WORKER_PROCESS_FAILED(5, "worker执行失败"),
    WORKER_PROCESS_SUCCESS(6, "worker执行成功"),

    /* ******************* Worker 专用 ******************* */
    RECEIVE_SUCCESS(11, "成功接受任务但未开始执行（此时worker满载，暂时无法运行）"),
    PROCESSING(12, "执行中"),
    PROCESS_FAILED(13, "执行失败"),
    PROCESS_SUCCESS(14, "执行成功");

    private int value;
    private String des;

    public static TaskStatus of(int v) {
        for (TaskStatus taskStatus : values()) {
            if (v == taskStatus.value) {
                return taskStatus;
            }
        }
        throw new IllegalArgumentException("no TaskStatus match the value of " + v);
    }

    public static TaskStatus convertStatus(TaskStatus processorStatus) {
        switch (processorStatus) {
            case RECEIVE_SUCCESS: return WORKER_RECEIVED;
            case PROCESSING: return WORKER_PROCESSING;
            case PROCESS_FAILED: return WORKER_PROCESS_FAILED;
            case PROCESS_SUCCESS: return WORKER_PROCESS_SUCCESS;
        }
        throw new IllegalArgumentException(processorStatus.name() + " is not the processor status.");
    }
}
