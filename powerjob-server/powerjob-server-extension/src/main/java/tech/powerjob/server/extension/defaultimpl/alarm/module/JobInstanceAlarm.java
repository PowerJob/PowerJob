package tech.powerjob.server.extension.defaultimpl.alarm.module;

import lombok.Data;
import org.apache.commons.lang.StringUtils;
import tech.powerjob.common.OmsConstant;
import tech.powerjob.common.utils.CommonUtils;

/**
 * 任务执行失败告警对象
 *
 * @author tjq
 * @since 2020/4/30
 */
@Data
public class JobInstanceAlarm implements Alarm {
    /**
     * 应用ID
     */
    private long appId;
    /**
     * 任务ID
     */
    private long jobId;
    /**
     * 任务实例ID
     */
    private long instanceId;
    /**
     * 任务名称
     */
    private String jobName;
    /**
     * 任务自带的参数
     */
    private String jobParams;
    /**
     * 时间表达式类型（CRON/API/FIX_RATE/FIX_DELAY）
     */
    private Integer timeExpressionType;
    /**
     * 时间表达式，CRON/NULL/LONG/LONG
     */
    private String timeExpression;
    /**
     * 执行类型，单机/广播/MR
     */
    private Integer executeType;
    /**
     * 执行器类型，Java/Shell
     */
    private Integer processorType;
    /**
     * 执行器信息
     */
    private String processorInfo;

    /**
     * 任务实例参数
     */
    private String instanceParams;
    /**
     * 执行结果
     */
    private String result;
    /**
     * 预计触发时间
     */
    private Long expectedTriggerTime;
    /**
     * 实际触发时间
     */
    private Long actualTriggerTime;
    /**
     * 结束时间
     */
    private Long finishedTime;
    /**
     * TaskTracker地址
     */
    private String taskTrackerAddress;

    @Override
    public String fetchTitle() {
        return "Chronos Alarm: Job Running Failed";
    }

    @Override
    public String fetchSimpleContent() {

        StringBuilder sb = new StringBuilder(4096);
        sb.append(fetchTitle()).append(OmsConstant.LINE_SEPARATOR);
        // JobInfo: $jobName($jobId)
        sb.append("JobInfo: ").append(jobName).append("(").append(jobId).append(")").append(OmsConstant.LINE_SEPARATOR);
        // InstanceId: $instanceId
        sb.append("InstanceId: ").append(instanceId).append(OmsConstant.LINE_SEPARATOR);
        if (StringUtils.isNotBlank(taskTrackerAddress)){
            // TaskTrackerAddress: $taskTrackerAddress
            sb.append("TaskTrackerAddress: ").append(taskTrackerAddress).append(OmsConstant.LINE_SEPARATOR);
        }
        // Result: $result
        sb.append("Result: ").append(result).append(OmsConstant.LINE_SEPARATOR);
        // FinishTime: $finishedTime
        sb.append("FinishTime: ").append(CommonUtils.formatTime((finishedTime))).append(OmsConstant.LINE_SEPARATOR);

        return sb.toString();
    }
}
