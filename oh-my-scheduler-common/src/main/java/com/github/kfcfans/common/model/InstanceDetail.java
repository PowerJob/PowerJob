package com.github.kfcfans.common.model;

import com.github.kfcfans.common.OmsSerializable;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 任务实例的运行详细信息（对外）
 *
 * @author tjq
 * @since 2020/4/11
 */
@Data
@NoArgsConstructor
public class InstanceDetail implements OmsSerializable {

    // 任务整体开始时间
    private Long actualTriggerTime;
    // 任务整体结束时间（可能不存在）
    private Long finishedTime;
    // 任务状态（中文）
    private String status;
    // 任务执行结果（可能不存在）
    private String result;
    // TaskTracker地址
    private String taskTrackerAddress;

    // MR或BD任务专用
    private TaskDetail taskDetail;
    // 秒级任务专用
    private List<SubInstanceDetail> subInstanceDetails;


    // 秒级任务的 extra -> List<SubInstanceDetail>
    @Data
    @NoArgsConstructor
    public static class SubInstanceDetail implements OmsSerializable {
        private long subInstanceId;
        private String startTime;
        private String finishedTime;
        private String result;
        private String status;
    }

    // MapReduce 和 Broadcast 任务的 extra ->
    @Data
    @NoArgsConstructor
    public static class TaskDetail implements OmsSerializable {
        private long totalTaskNum;
        private long succeedTaskNum;
        private long failedTaskNum;
    }
}
