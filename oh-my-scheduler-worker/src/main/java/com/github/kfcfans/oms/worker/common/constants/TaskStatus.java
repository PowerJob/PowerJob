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
    DISPATCH_SUCCESS(2, "调度成功"),
    DISPATCH_FAILED(3, "调度失败"),
    WORKER_PROCESS_SUCCESS(4, "worker执行成功"),
    WORKER_PROCESS_FAILED(5, "worker执行失败");


    /* ******************* Worker 专用 ******************* */

    private int value;
    private String des;
}
