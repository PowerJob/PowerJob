package tech.powerjob.server.extension.defaultimpl.alarm.module;

import com.google.common.collect.Maps;
import tech.powerjob.common.OmsConstant;
import tech.powerjob.common.model.PEWorkflowDAG;
import lombok.Data;
import tech.powerjob.common.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 工作流执行失败告警对象
 *
 * @author tjq
 * @since 2020/6/12
 */
@Data
public class WorkflowInstanceAlarm implements Alarm {

    private String wfName;
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
        return "Chronos Alarm: Workflow Running Failed";
    }

    @Override
    public String fetchSimpleContent() {

        StringBuilder sb = new StringBuilder(4096);
        sb.append(fetchTitle()).append(OmsConstant.LINE_SEPARATOR);
        // Workflow: $workflowName($workflowId)
        sb.append("Workflow:  ").append(wfName).append("(").append(workflowId).append(")").append(OmsConstant.LINE_SEPARATOR);
        // InstanceId: $instanceId
        sb.append("InstanceId:  ").append(wfInstanceId).append(OmsConstant.LINE_SEPARATOR);
        // Result: $result
        sb.append("Result:  ").append(result).append(OmsConstant.LINE_SEPARATOR);
        // FinishTime: $finishedTime
        sb.append("FinishTime:  ").append(CommonUtils.formatTime((finishedTime))).append(OmsConstant.LINE_SEPARATOR);
        return sb.toString();
    }

    @Override
    public Map<String, String> fetchContentMap() {
        HashMap<String, String> map = Maps.newLinkedHashMap();
        map.put("Workflow",wfName);
        map.put("WorkflowId",String.valueOf(workflowId));
        map.put("wfInstanceId",String.valueOf(wfInstanceId));
        map.put("ActualTriggerTime",CommonUtils.formatTime(actualTriggerTime));
        map.put("FinishTime",CommonUtils.formatTime(finishedTime));
        map.put("Result",result);
        return map;
    }
}
