package tech.powerjob.server.core.scheduler.auxiliary;

import tech.powerjob.common.enums.TimeExpressionType;

/**
 * @author Echo009
 * @since 2022/2/24
 */
public interface TimingStrategyHandler {

    /**
     * 校验表达式
     *
     * @param timeExpression 时间表达式
     */
    void validate(String timeExpression);

    /**
     * 计算下次触发时间
     *
     * @param preTriggerTime 上次触发时间 (not null)
     * @param timeExpression 时间表达式
     * @param startTime      开始时间(include)
     * @param endTime        结束时间(include)
     * @return next trigger time
     */
    Long calculateNextTriggerTime(Long preTriggerTime, String timeExpression, Long startTime, Long endTime);

    /**
     * 支持的定时策略
     *
     * @return TimeExpressionType
     */
    TimeExpressionType supportType();


}
