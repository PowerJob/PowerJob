package com.github.kfcfans.oms.server.web.request;

import lombok.Data;

/**
 * 创建/修改 JobInfo 请求
 *
 * @author tjq
 * @since 2020/3/30
 */
@Data
public class ModifyJobInfoRequest {

    /* ************************** 任务基本信息 ************************** */
    // 任务名称
    private String jobName;
    // 任务描述
    private String jobDescription;
    // 任务所属的应用ID
    private Long appId;
    // 任务分组名称（仅用于前端展示的分组）
    private String groupName;

    /* ************************** 定时参数 ************************** */
    // 时间表达式类型（CRON/API/FIX_RATE/FIX_DELAY）
    private String timeExpressionType;
    // 时间表达式，CRON/NULL/LONG/LONG
    private String timeExpression;


    /* ************************** 执行方式 ************************** */
    // 执行类型，单机/广播/MR
    private String executeType;
    // 执行器类型，Java/Shell
    private String processorType;
    // 执行器信息
    private String processorInfo;

    /* ************************** 运行时配置 ************************** */
    // 并发度，同时执行的线程数量
    private Integer concurrency;
    // 任务整体超时时间
    private Long instanceTimeLimit;
    // 任务的每一个Task超时时间
    private Long taskTimeLimit;
}
