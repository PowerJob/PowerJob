package com.github.kfcfans.oms.server.model;

/**
 * 任务运行日志表
 *
 * @author tjq
 * @since 2020/3/30
 */
public class JobLogDO extends BaseDO {

    // 任务ID
    private Long jobId;
    // 任务实例ID
    private String instanceId;
    // 任务状态 运行中/成功/失败...
    private int status;
    // 执行结果
    private String result;
    // 耗时
    private Long usedTime;

}
