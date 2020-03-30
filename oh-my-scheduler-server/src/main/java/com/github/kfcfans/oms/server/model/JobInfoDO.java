package com.github.kfcfans.oms.server.model;

/**
 * 任务信息表
 *
 * @author tjq
 * @since 2020/3/29
 */
public class JobInfoDO extends BaseDO {


    /* ************************** 任务基本信息 ************************** */
    // 任务名称
    private String jobName;
    // 任务描述
    private String jobDescription;
    // 任务分组名称
    private String groupName;

    /* ************************** 定时参数 ************************** */
    // 时间表达式类型（CRON/API/FIX_RATE/FIX_DELAY）
    private int timeExpressionType;
    // 时间表达式，CRON/NULL/LONG/LONG
    private String timeExpression;


    /* ************************** 执行方式 ************************** */
    // 执行类型，单机/广播/MR
    private int executeType;
    // 执行器类型，Java/Shell
    private int processorType;
    // 执行器信息
    private String processorInfo;

    /* ************************** 运行时配置 ************************** */
    // 并发度，同时执行的线程数量
    private int concurrency;
    // 任务整体超时时间
    private long instanceTimeLimit;
    // 任务的每一个Task超时时间
    private long taskTimeLimit;

}
