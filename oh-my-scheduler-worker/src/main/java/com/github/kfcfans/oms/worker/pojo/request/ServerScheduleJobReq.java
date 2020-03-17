package com.github.kfcfans.oms.worker.pojo.request;

import lombok.Data;

/**
 * 服务端调度任务请求（一次任务处理的入口）
 *
 * @author tjq
 * @since 2020/3/17
 */
@Data
public class ServerScheduleJobReq {

    // 调度的服务器地址，默认通讯目标
    private String serverAddress;

    /* *********************** 任务相关属性 *********************** */

    private String jobId;
    private String instanceId;
    // 任务执行类型，单机、广播、MR
    private String executeType;
    // 处理器类型（JavaBean、Jar、脚本等）
    private String processorType;
    // 处理器信息
    private String processorInfo;
    // 任务执行时间限制，单位毫秒
    private long timeLimit;
    // 可用处理器地址，可能多值，逗号分隔
    private String workerAddress;

    private String jobParams;
    private String instanceParams;

    /* *********************** Map/MapReduce 任务专用 *********************** */

    // 每台机器的处理线程数上限
    private int threadConcurrency;
    // 子任务重试次数（任务本身的重试机制由server控制）
    private int taskRetryNum;
}
