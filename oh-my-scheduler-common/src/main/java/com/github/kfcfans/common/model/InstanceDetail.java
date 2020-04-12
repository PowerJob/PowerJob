package com.github.kfcfans.common.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 任务实例的运行详细信息（对外）
 *
 * @author tjq
 * @since 2020/4/11
 */
@Data
public class InstanceDetail implements Serializable {

    // 任务整体开始时间
    private long actualTriggerTime;
    // 任务整体结束时间（可能不存在）
    private long finishedTime;
    // 任务状态（中文）
    private String status;
    // 任务执行结果（可能不存在）
    private String result;
    // TaskTracker地址
    private String taskTrackerAddress;

    private Object extra;


    // 秒级任务的 extra -> List<SubInstanceDetail>
    @Data
    public static class SubInstanceDetail implements Serializable {
        private long startTime;
        private long finishedTime;
        private String result;
        private String status;
    }

    // MapReduce 和 Broadcast 任务的 extra ->
    @Data
    public static class TaskDetail implements Serializable {
        private long totalTaskNum;
        private long succeedTaskNum;
        private long failedTaskNum;
    }
}
