package tech.powerjob.worker.pojo.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 被调度执行的任务实例详情
 *
 * @author tjq
 * @since 2020/3/16
 */
@Data
public class InstanceInfo implements Serializable {

    /**
     * 基础信息
     */
    private Long jobId;
    private Long instanceId;
    private Long wfInstanceId;

    /**
     * 任务执行处理器信息
     */
    // 任务执行类型，单机、广播、MR
    private String executeType;
    // 处理器类型（JavaBean、Jar、脚本等）
    private String processorType;
    // 处理器信息
    private String processorInfo;
    // 定时类型
    private int timeExpressionType;

    /**
     * 超时时间
     */
    // 整个任务的总体超时时间
    private long instanceTimeoutMS;

    /**
     * 任务运行参数
     */
    // 任务级别的参数，相当于类的static变量
    private String jobParams;
    // 实例级别的参数，相当于类的普通变量
    private String instanceParams;


    // 每台机器的处理线程数上限
    private int threadConcurrency;
    // 子任务重试次数（任务本身的重试机制由server控制）
    private int taskRetryNum;

    private String logConfig;

    private String advancedRuntimeConfig;
}
