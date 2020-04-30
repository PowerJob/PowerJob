package com.github.kfcfans.oms.server.service.alarm;

import lombok.Data;

/**
 * 告警对象
 *
 * @author tjq
 * @since 2020/4/30
 */
@Data
public class AlarmContent {
    // 应用ID
    private long appId;
    // 任务ID
    private long jobId;
    // 任务实例ID
    private long instanceId;
    // 任务名称
    private String jobName;
}
