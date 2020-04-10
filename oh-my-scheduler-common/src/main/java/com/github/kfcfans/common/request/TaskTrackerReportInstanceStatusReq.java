package com.github.kfcfans.common.request;

import lombok.Data;

import java.io.Serializable;

/**
 * TaskTracker 将状态上报给服务器
 *
 * @author tjq
 * @since 2020/3/17
 */
@Data
public class TaskTrackerReportInstanceStatusReq implements Serializable {

    private Long jobId;
    private Long instanceId;

    private int instanceStatus;

    private String result;

    /* ********* 统计信息 ********* */
    private long totalTaskNum;
    private long succeedTaskNum;
    private long failedTaskNum;

    private long startTime;
    private long reportTime;
    private String sourceAddress;
}
