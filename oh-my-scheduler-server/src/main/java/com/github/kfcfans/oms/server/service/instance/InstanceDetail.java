package com.github.kfcfans.oms.server.service.instance;

import lombok.Data;

/**
 * 任务实例的运行详细信息（对外）
 *
 * @author tjq
 * @since 2020/4/11
 */
@Data
public class InstanceDetail {

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
    private static class SubInstanceDetail {
        private long startTime;
        private long finishedTime;
        private String status;
        private String result;
    }

    // MapReduce 和 Broadcast 任务的 extra ->
    private static class ClusterDetail {
        private long totalTaskNum;
        private long succeedTaskNum;
        private long failedTaskNum;
    }
}
