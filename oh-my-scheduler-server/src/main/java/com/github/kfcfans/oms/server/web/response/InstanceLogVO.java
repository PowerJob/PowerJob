package com.github.kfcfans.oms.server.web.response;

import lombok.Data;

import java.util.Date;

/**
 * ExecuteLog 对外展示对象
 *
 * @author tjq
 * @since 2020/4/12
 */
@Data
public class InstanceLogVO {

    // 任务ID
    private Long jobId;
    // 任务名称
    private String jobName;
    // 任务实例ID
    private Long instanceId;

    // 执行结果
    private String result;

    // TaskTracker地址
    private String taskTrackerAddress;

    // 总共执行的次数（用于重试判断）
    private Long runningTimes;

    /* ********** 不一致区域 ********** */
    private String status;
    // 实际触发时间（需要格式化为人看得懂的时间）
    private String actualTriggerTime;
    // 结束时间（同理，需要格式化）
    private String finishedTime;
}
