package tech.powerjob.common.request;

import tech.powerjob.common.PowerSerializable;
import lombok.Data;

import java.util.Map;


/**
 * TaskTracker 将状态上报给服务器
 *
 * @author tjq
 * @since 2020/3/17
 */
@Data
public class TaskTrackerReportInstanceStatusReq implements PowerSerializable {

    /**
     * 追加上报自己的 appId
     * 方便后续的监控日志埋点
     */
    private Long appId;

    private Long jobId;

    private Long instanceId;

    private Long wfInstanceId;
    /**
     * 追加的工作流上下文数据
     * @since 2021/02/05
     */
    private Map<String,String> appendedWfContext;

    private int instanceStatus;

    private String result;

    /* ********* 统计信息 ********* */

    private long totalTaskNum;

    private long succeedTaskNum;

    private long failedTaskNum;

    private long startTime;

    private Long endTime;

    private long reportTime;

    private String sourceAddress;

    /* ********* 秒级任务的告警信息 ********* */

    private boolean needAlert;

    private String alertContent;



}
