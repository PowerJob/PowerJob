package tech.powerjob.common.response;

import tech.powerjob.common.model.PEWorkflowDAG;
import lombok.Data;

import java.util.Date;

/**
 * workflowInfo 对外输出对象
 *
 * @author tjq
 * @since 2020/6/2
 */
@Data
public class WorkflowInfoDTO {

    private Long id;

    private String wfName;

    private String wfDescription;

    /**
     * 所属应用ID
     */
    private Long appId;

    /**
     * 工作流的DAG图信息（点线式DAG的json）
     */
    private PEWorkflowDAG pEWorkflowDAG;

    /* ************************** 定时参数 ************************** */

    /**
     * 时间表达式类型（CRON/API/FIX_RATE/FIX_DELAY）
     */
    private String timeExpressionType;
    /**
     * 时间表达式，CRON/NULL/LONG/LONG
     */
    private String timeExpression;

    /**
     * 最大同时运行的工作流个数，默认 1
     */
    private Integer maxWfInstanceNum;

    /**
     * 1 正常运行，2 停止（不再调度）
     */
    private Integer status;
    /**
     * 下一次调度时间
     */
    private Long nextTriggerTime;

    /**
     * 工作流整体失败的报警
     */
    private String notifyUserIds;

    private Date gmtCreate;

    private Date gmtModified;

    /**
     * ENABLE / DISABLE
     *
     * status 字段的转义
     */
    private Boolean enable;
}
