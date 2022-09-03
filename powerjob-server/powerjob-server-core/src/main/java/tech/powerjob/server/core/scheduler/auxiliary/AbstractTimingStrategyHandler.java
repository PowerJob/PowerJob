package tech.powerjob.server.core.scheduler.auxiliary;


/**
 * @author Echo009
 * @since 2022/3/22
 */
public abstract class AbstractTimingStrategyHandler implements TimingStrategyHandler {
    @Override
    public void validate(String timeExpression) {
        // do nothing
    }

    @Override
    public Long calculateNextTriggerTime(Long preTriggerTime, String timeExpression, Long startTime, Long endTime) {
        // do nothing
        return null;
    }
}
