package tech.powerjob.server.core.alarm.module;

import tech.powerjob.common.model.PEWorkflowDAG;
import lombok.Data;
import tech.powerjob.server.extension.alarm.Alarm;

/**
 * 工作流执行失败告警对象
 *
 * @author tjq
 * @since 2020/6/12
 */
@Data
public class WorkflowInstanceAlarm implements Alarm {

    private String workflowName;

    /**
     * 任务所属应用的ID，冗余提高查询效率
     */
    private Long appId;
    private Long workflowId;
    /**
     * workflowInstanceId（任务实例表都使用单独的ID作为主键以支持潜在的分表需求）
     */
    private Long wfInstanceId;
    /**
     *  workflow 状态（WorkflowInstanceStatus）
     */
    private Integer status;

    private PEWorkflowDAG peWorkflowDAG;
    private String result;

    /**
     * 实际触发时间
     */
    private Long actualTriggerTime;
    /**
     * 结束时间
     */
    private Long finishedTime;

    /**
     * 时间表达式类型（CRON/API/FIX_RATE/FIX_DELAY）
     */
    private Integer timeExpressionType;
    /**
     * 时间表达式，CRON/NULL/LONG/LONG
     */
    private String timeExpression;

    @Override
    public String fetchTitle() {
        return "PowerJob AlarmService: Workflow Running Failed";
    }
}
