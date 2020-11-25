package com.github.kfcfans.powerjob.common;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Workflow 任务运行状态
 *
 * @author tjq
 * @since 2020/5/26
 */
@Getter
@AllArgsConstructor
public enum WorkflowInstanceStatus {

    WAITING(1, "等待调度"),
    RUNNING(2, "运行中"),
    FAILED(3, "失败"),
    SUCCEED(4, "成功"),
    STOPPED(10, "手动停止");

    // 广义的运行状态
    public static final List<Integer> generalizedRunningStatus = Lists.newArrayList(WAITING.v, RUNNING.v);
    // 结束状态
    public static final List<Integer> finishedStatus = Lists.newArrayList(FAILED.v, SUCCEED.v, STOPPED.v);

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
