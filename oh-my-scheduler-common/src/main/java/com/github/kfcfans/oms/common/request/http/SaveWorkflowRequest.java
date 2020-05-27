package com.github.kfcfans.oms.common.request.http;

import com.github.kfcfans.oms.common.model.PEWorkflowDAG;
import lombok.Data;

import java.util.List;

/**
 * 创建/修改 Workflow 请求
 *
 * @author tjq
 * @since 2020/5/26
 */
@Data
public class SaveWorkflowRequest {

    private Long id;

    private String wfName;
    private String wfDescription;

    // 所属应用ID
    private Long appId;

    // 点线表示法
    private PEWorkflowDAG pEWorkflowDAG;

    /* ************************** 定时参数 ************************** */
    // 时间表达式类型（CRON/API/FIX_RATE/FIX_DELAY）
    private String timeExpressionType;
    // 时间表达式，CRON/NULL/LONG/LONG
    private String timeExpression;

    // 最大同时运行的工作流个数，默认 1
    private Integer maxWfInstanceNum;

    // ENABLE / DISABLE
    private String status;

    // 工作流整体失败的报警
    private List<Long> notifyUserIds;
}
