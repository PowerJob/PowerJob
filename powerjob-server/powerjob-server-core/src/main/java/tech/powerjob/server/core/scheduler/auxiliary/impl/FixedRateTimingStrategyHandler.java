package tech.powerjob.server.core.scheduler.auxiliary.impl;

import org.springframework.stereotype.Component;
import tech.powerjob.common.PowerJobDKey;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.server.core.scheduler.auxiliary.AbstractTimingStrategyHandler;


/**
 * @author Echo009
 * @since 2022/3/22
 */
@Component
public class FixedRateTimingStrategyHandler extends AbstractTimingStrategyHandler {

    @Override
    public void validate(String timeExpression) {
        long delay;
        try {
            delay = Long.parseLong(timeExpression);
        } catch (Exception e) {
            throw new PowerJobException("invalid timeExpression!");
        }
        // 默认 120s ，超过这个限制应该使用考虑使用其他类型以减少资源占用
        int maxInterval = Integer.parseInt(System.getProperty(PowerJobDKey.FREQUENCY_JOB_MAX_INTERVAL, "120000"));
        if (delay > maxInterval) {
            throw new PowerJobException("the rate must be less than " + maxInterval + "ms");
        }
        if (delay <= 0) {
            throw new PowerJobException("the rate must be greater than 0 ms");
        }
    }

    @Override
    public Long calculateNextTriggerTime(Long preTriggerTime, String timeExpression, Long startTime, Long endTime) {
        long r = startTime != null && startTime > preTriggerTime
                ? startTime : preTriggerTime + Long.parseLong(timeExpression);
        return endTime != null && endTime < r ? null : r;
    }

    @Override
    public TimeExpressionType supportType() {
        return TimeExpressionType.FIXED_RATE;
    }
}
