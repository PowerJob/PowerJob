package com.github.kfcfans.oms.common.request.http;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 创建/修改 JobInfo 请求
 *
 * @author tjq
 * @since 2020/3/30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobInfoRequest {

    // 任务ID（jobId），null -> 插入，否则为更新
    private Long id;
    /* ************************** 任务基本信息 ************************** */
    // 任务名称
    private String jobName;
    // 任务描述
    private String jobDescription;
    // 任务所属的应用ID
    private Long appId;
    // 任务分组名称（仅用于前端展示的分组）
    private String groupName;
    // 任务自带的参数
    private String jobParams;

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
    // 最大同时运行任务数
    private Integer maxInstanceNum;
    // 并发度，同时执行的线程数量
    private Integer concurrency;
    // 任务整体超时时间
    private Long instanceTimeLimit;

    /* ************************** 重试配置 ************************** */
    private Integer instanceRetryNum;
    private Integer taskRetryNum;

    /* ************************** 繁忙机器配置 ************************** */
    // 最低CPU核心数量，0代表不限
    private double minCpuCores;
    // 最低内存空间，单位 GB，0代表不限
    private double minMemorySpace;
    // 最低磁盘空间，单位 GB，0代表不限
    private double minDiskSpace;

    // 1 正常运行，2 停止（不再调度）
    private boolean enable;


    /* ************************** 集群配置 ************************** */
    // 指定机器运行，空代表不限，非空则只会使用其中的机器运行（多值逗号分割）
    private String designatedWorkers;
    // 最大机器数量
    private Integer maxWorkerCount;

    // 报警用户ID列表
    private List<Long> notifyUserIds;
}
