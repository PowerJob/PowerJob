package com.github.kfcfans.oms.worker.pojo.model;

import lombok.Data;

/**
 * 被调度执行的任务实例详情
 *
 * @author tjq
 * @since 2020/3/16
 */
@Data
public class JobInstanceInfo {

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
    private String allWorkerAddress;

    private String jobParams;
    private String instanceParams;

    /* *********************** Map/MapReduce 任务专用 *********************** */

    // 每台机器的处理线程数上限
    private int threadConcurrency;
    // 子任务重试次数（任务本身的重试机制由server控制）
    private int taskRetryNum;
}
