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

    /**
     * Expected trigger time.
     */
    private Long expectedTriggerTime;
    /**
     * Actual trigger time of an instance.
     */
    private Long actualTriggerTime;
    // 任务整体结束时间（可能不存在）
    /**
     * Finish time of an instance, which may be null.
     */
    private Long finishedTime;
    /**
     * Status of the task instance.
     */
    private Integer status;
    // 任务执行结果（可能不存在）
    /**
     * Execution result, which may be null.
     */
    private String result;
    // TaskTracker地址
    /**
     * Task tracker address.
     */
    private String taskTrackerAddress;
    // 启动参数
    /**
     * Param string that is passed to an instance when it is initialized.
     */
    private String instanceParams;

    // MR或BD任务专用
    /**
     * Task detail, used by MapReduce or Broadcast tasks.
     */
    private TaskDetail taskDetail;
    // 秒级任务专用
    /**
     *
     */
    private List<SubInstanceDetail> subInstanceDetails;

    // 重试次数
    /**
     * 
     */
    private Long runningTimes;

    // 扩展字段，中间件升级不易，最好不要再改 common 包了...否则 server worker 版本不兼容
    /**
     * Extend
     */
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
