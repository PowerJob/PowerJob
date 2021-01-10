package com.github.kfcfans.powerjob.common;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Status of the job instance
 *
 * @author tjq
 * @since 2020/3/17
 */
@Getter
@AllArgsConstructor
public enum InstanceStatus {

    WAITING_DISPATCH(1, "等待派发"),
    WAITING_WORKER_RECEIVE(2, "等待Worker接收"),
    RUNNING(3, "运行中"),
    FAILED(4, "失败"),
    SUCCEED(5, "成功"),
    CANCELED(9, "取消"),
    STOPPED(10, "手动停止");

    private final int v;
    private final String des;

    // 广义的运行状态
    public static final List<Integer> generalizedRunningStatus = Lists.newArrayList(WAITING_DISPATCH.v, WAITING_WORKER_RECEIVE.v, RUNNING.v);
    // 结束状态
    public static final List<Integer> finishedStatus = Lists.newArrayList(FAILED.v, SUCCEED.v, CANCELED.v, STOPPED.v);

    public static InstanceStatus of(int v) {
        for (InstanceStatus is : values()) {
            if (v == is.v) {
                return is;
            }
        }
        throw new IllegalArgumentException("InstanceStatus has no item for value " + v);
    }
}
