package com.github.kfcfans.common.request;

import com.github.kfcfans.common.OmsSerializable;
import lombok.Data;


/**
 * TaskTracker 将状态上报给服务器
 *
 * @author tjq
 * @since 2020/3/17
 */
@Data
public class TaskTrackerReportInstanceStatusReq implements OmsSerializable {

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
