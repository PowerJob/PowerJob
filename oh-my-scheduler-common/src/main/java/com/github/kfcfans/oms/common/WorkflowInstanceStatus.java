package com.github.kfcfans.oms.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Workflow 任务运行状态
 *
 * @author tjq
 * @since 2020/5/26
 */
@Getter
@AllArgsConstructor

public enum WorkflowInstanceStatus {

    RUNNING(1, "运行中"),
    FAILED(2, "失败"),
    SUCCEED(3, "成功"),
    STOPPED(10, "手动停止");

    private int v;
    private String des;

    public static WorkflowInstanceStatus of(int v) {
        for (WorkflowInstanceStatus is : values()) {
            if (v == is.v) {
                return is;
            }
        }
        throw new IllegalArgumentException("WorkflowInstanceStatus has no item for value " + v);
    }
}
