package tech.powerjob.server.core.alarm.module;

import lombok.Data;
import lombok.experimental.Accessors;
import tech.powerjob.server.extension.alarm.Alarm;

/**
 * 任务执行失败告警对象
 *
 * @author tjq
 * @since 2020/4/30
 */
@Data
@Accessors(chain = true)
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
     *  任务名称
     */
    private String jobName;
    /**
     * 任务自带的参数
     */
    private String jobParams;
    /**
     *  时间表达式类型（CRON/API/FIX_RATE/FIX_DELAY）
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
     *
     */
    private String taskTrackerAddress;

    @Override
    public String fetchTitle() {
        return "PowerJob AlarmService: Job Running Failed";
    }
}
