package com.github.kfcfans.oms.server.core;

import lombok.Data;

/**
 * 保存任务实例的状态信息
 *
 * @author tjq
 * @since 2020/4/7
 */
@Data
public class InstanceStatusHolder {

    private long instanceId;
    private int instanceStatus;
    private String result;

    /* ********* 统计信息 ********* */
    private long totalTaskNum;
    private long succeedTaskNum;
    private long failedTaskNum;

    // 上次上报时间
    private long lastReportTime;
}
