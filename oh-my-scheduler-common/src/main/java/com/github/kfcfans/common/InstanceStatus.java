package com.github.kfcfans.common;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 任务运行状态
 *
 * @author tjq
 * @since 2020/3/17
 */
@Getter
@AllArgsConstructor
public enum InstanceStatus {

    WAITING_DISPATCH(1, "等待任务派发"),
    WAITING_WORKER_RECEIVE(2, "Server已完成任务派发，等待Worker接收"),
    RUNNING(3, "Worker接收成功，正在运行任务"),
    FAILED(4, "任务运行失败"),
    SUCCEED(5, "任务运行成功"),
    STOPPED(10, "任务被手动停止");

    private int v;
    private String des;

    // 广义的运行状态
    public static final List<Integer> generalizedRunningStatus = Lists.newArrayList(WAITING_DISPATCH.v, WAITING_WORKER_RECEIVE.v, RUNNING.v);

    public static InstanceStatus of(int v) {
        for (InstanceStatus is : values()) {
            if (v == is.v) {
                return is;
            }
        }
        throw new IllegalArgumentException("InstanceStatus has no item for value " + v);
    }
}
