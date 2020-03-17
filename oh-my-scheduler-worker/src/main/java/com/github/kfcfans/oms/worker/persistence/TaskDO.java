package com.github.kfcfans.oms.worker.persistence;

import lombok.Data;

/**
 * TaskDO（为了简化 DAO 层，一张表实现两种功能）
 * 对于 TaskTracker，task_info 存储了当前 JobInstance 所有的子任务信息
 * 对于普通的 Worker，task_info 存储了当前无法处理的任务信息
 *
 * @author tjq
 * @since 2020/3/17
 */
@Data
public class TaskDO {

    // 层次命名法，可以表示 Map 后的父子关系，如 0.1.2 代表 rootTask map 的第一个 task map 的第二个 task
    private String taskId;

    private String jobId;
    private String instanceId;
    // 任务名称
    private String taskName;
    // 任务参数
    private String taskContent;
    // 对于JobTracker为workerAddress，对于普通Worker为jobTrackerAddress
    private String address;
    // 任务状态，0～10代表 JobTracker 使用，11～20代表普通Worker使用
    private int status;
    // 执行结果
    private String result;
    // 创建时间
    private long createdTime;
    // 最后修改时间
    private long lastModifiedTime;
}
