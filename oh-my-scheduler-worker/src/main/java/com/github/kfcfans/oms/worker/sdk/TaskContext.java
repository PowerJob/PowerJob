package com.github.kfcfans.oms.worker.sdk;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * 任务上下文
 * 概念统一，所有的worker只处理Task，Job和JobInstance的概念只存在于Server和TaskTracker
 * 单机任务 -> 整个Job变成一个Task
 * 广播任务 -> 整个jOb变成一堆一样的Task
 * MR 任务 -> 被map出来的任务都视为根Task的子Task
 *
 * @author tjq
 * @since 2020/3/18
 */
@Getter
@Setter
public class TaskContext {

    private String jobId;
    private String instanceId;
    private String taskId;
    private String taskName;

    private String jobParams;
    private String instanceParams;

    private int maxRetryTimes;
    private int currentRetryTimes;

    private Object subTask;

    private String taskTrackerAddress;


    public String getDescription() {
        return "jobId='" + jobId + '\'' +
                ", instanceId='" + instanceId + '\'' +
                ", taskId='" + taskId + '\'' +
                ", taskName='" + taskName + '\'' +
                ", jobParams='" + jobParams + '\'' +
                ", instanceParams='" + instanceParams + '\'' +
                ", taskTrackerAddress='" + taskTrackerAddress;
    }
}
