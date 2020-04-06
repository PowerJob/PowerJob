package com.github.kfcfans.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务运行状态
 *
 * @author tjq
 * @since 2020/3/17
 */
@Getter
@AllArgsConstructor
public enum InstanceStatus {

    WAITING_DISPATCH(1, "等待任务派发，任务处理Server时间轮中"),
    WAITING_WORKER_RECEIVE(2, "Server已完成任务派发，等待Worker接收"),
    RUNNING(3, "Worker接收成功，正在运行任务"),
    FAILED(4, "任务运行失败"),
    SUCCEED(5, "任务运行成功"),
    STOPPED(10, "任务被手动停止");

    private int v;
    private String des;
}
