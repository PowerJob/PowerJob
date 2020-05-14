package com.github.kfcfans.oms.client.model;

import com.github.kfcfans.oms.common.ExecuteType;
import com.github.kfcfans.oms.common.ProcessorType;
import com.github.kfcfans.oms.common.TimeExpressionType;
import com.google.common.collect.Lists;
import lombok.Data;

import java.util.List;

/**
 * oms-client 使用的 JobInfo 对象，用于创建/更新 任务
 * id == null -> 创建
 * id != null -> 更新，注：更新为全字段覆盖，即需要保证该对象包含所有参数（不能仅传入更新字段）
 *
 * @author tjq
 * @since 2020/5/14
 */
@Data
public class ClientJobInfo {

    // null -> 新增，否则为更新
    private Long jobId;
    /* ************************** 任务基本信息 ************************** */
    // 任务名称
    private String jobName;
    // 任务描述
    private String jobDescription;
    // 任务自带的参数
    private String jobParams;

    /* ************************** 定时参数 ************************** */
    // 时间表达式类型（CRON/API/FIX_RATE/FIX_DELAY）
    private TimeExpressionType timeExpressionType;
    // 时间表达式，CRON/NULL/LONG/LONG
    private String timeExpression;

    /* ************************** 执行方式 ************************** */
    // 执行类型，单机/广播/MR
    private ExecuteType executeType;
    // 执行器类型，Java/Shell
    private ProcessorType processorType;
    // 执行器信息
    private String processorInfo;

    /* ************************** 运行时配置 ************************** */
    // 最大同时运行任务数，默认 1
    private Integer maxInstanceNum = 1;
    // 并发度，同时执行某个任务的最大线程数量
    private Integer concurrency = 5;
    // 任务整体超时时间
    private Long instanceTimeLimit = 0L;

    /* ************************** 重试配置 ************************** */
    private Integer instanceRetryNum = 0;
    private Integer taskRetryNum = 0;

    /* ************************** 繁忙机器配置 ************************** */
    // 最低CPU核心数量，0代表不限
    private double minCpuCores = 0;
    // 最低内存空间，单位 GB，0代表不限
    private double minMemorySpace = 0;
    // 最低磁盘空间，单位 GB，0代表不限
    private double minDiskSpace = 0;

    /* ************************** 集群配置 ************************** */
    // 指定机器运行，空代表不限，非空则只会使用其中的机器运行（多值逗号分割）
    private List<String> designatedWorkers = Lists.newLinkedList();
    // 最大机器数量，<=0 代表无限制
    private Integer maxWorkerCount = 0;

    // 报警用户ID列表，多值逗号分隔
    private List<Long> notifyUserIds = Lists.newLinkedList();

    // 是否启用任务
    private boolean enable = true;
}
