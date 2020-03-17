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
    DISPATCH_SUCCESS(2, "调度成功（但不保证worker收到）"),
    WORKER_PROCESSING(3, "worker开始执行"),
    WORKER_PROCESS_SUCCESS(4, "worker执行成功"),
    WORKER_PROCESS_FAILED(5, "worker执行失败"),

    /* ******************* Worker 专用 ******************* */
    RECEIVE_SUCCESS(11, "成功接受任务但未开始执行（此时worker满载，暂时无法运行）"),
    PROCESSING(12, "执行中"),
    PROCESS_SUCCESS(13, "执行成功"),
    PROCESS_FAILED(14, "执行失败");

    private int value;
    private String des;

    public static TaskStatus of(int v) {
        switch (v) {
            case 1: return WAITING_DISPATCH;
            case 2: return DISPATCH_SUCCESS;
            case 3: return WORKER_PROCESSING;
            case 4: return WORKER_PROCESS_SUCCESS;
            case 5: return WORKER_PROCESS_FAILED;

            case 11: return RECEIVE_SUCCESS;
            case 12: return PROCESSING;
            case 13: return PROCESS_SUCCESS;
            case 14: return PROCESS_FAILED;
        }
        throw new IllegalArgumentException("no TaskStatus match the value of " + v);
    }
}
