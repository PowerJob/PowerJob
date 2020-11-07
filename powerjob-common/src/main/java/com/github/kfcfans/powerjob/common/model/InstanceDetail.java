package com.github.kfcfans.powerjob.common.model;

import com.github.kfcfans.powerjob.common.OmsSerializable;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 任务实例的运行详细信息
 *
 * @author tjq
 * @since 2020/4/11
 */
@Data
@NoArgsConstructor
public class InstanceDetail implements OmsSerializable {

    // 任务预计执行时间
    private Long expectedTriggerTime;
    // 任务整体开始时间
    private Long actualTriggerTime;
    // 任务整体结束时间（可能不存在）
    private Long finishedTime;
    // 任务状态
    private Integer status;
    // 任务执行结果（可能不存在）
    private String result;
    // TaskTracker地址
    private String taskTrackerAddress;
    // 启动参数
    private String instanceParams;

    // MR或BD任务专用
    private TaskDetail taskDetail;
    // 秒级任务专用
    private List<SubInstanceDetail> subInstanceDetails;

    // 重试次数
    private Long runningTimes;

    // 扩展字段，中间件升级不易，最好不要再改 common 包了...否则 server worker 版本不兼容
    private String extra;

    // 秒级任务的 extra -> List<SubInstanceDetail>
    @Data
    @NoArgsConstructor
    public static class SubInstanceDetail implements OmsSerializable {
        private long subInstanceId;
        private Long startTime;
        private Long finishedTime;
        private String result;
        private int status;
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
