package tech.powerjob.server.core.scheduler.auxiliary.impl;

import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import org.springframework.stereotype.Component;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.server.core.scheduler.auxiliary.TimingStrategyHandler;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * @author Echo009
 * @since 2022/2/24
 */
@Component
public class CronTimingStrategyHandler implements TimingStrategyHandler {

    private final CronParser cronParser;

    /**
     * @see CronDefinitionBuilder#instanceDefinitionFor
     * <p>
     * Enhanced quartz cron，Support for specifying both a day-of-week and a day-of-month parameter.
     * https://github.com/PowerJob/PowerJob/issues/382
     */
    public CronTimingStrategyHandler() {
        CronDefinition cronDefinition = CronDefinitionBuilder.defineCron()
                .withSeconds().withValidRange(0, 59).and()
                .withMinutes().withValidRange(0, 59).and()
                .withHours().withValidRange(0, 23).and()
                .withDayOfMonth().withValidRange(1, 31).supportsL().supportsW().supportsLW().supportsQuestionMark().and()
                .withMonth().withValidRange(1, 12).and()
                .withDayOfWeek().withValidRange(1, 7).withMondayDoWValue(2).supportsHash().supportsL().supportsQuestionMark().and()
                .withYear().withValidRange(1970, 2099).withStrictRange().optional().and()
                .instance();
        this.cronParser = new CronParser(cronDefinition);
    }


    @Override
    public void validate(String timeExpression) {
        cronParser.parse(timeExpression);
    }

    @Override
    public Long calculateNextTriggerTime(Long preTriggerTime, String timeExpression, Long startTime, Long endTime) {
        Cron cron = cronParser.parse(timeExpression);
        ExecutionTime executionTime = ExecutionTime.forCron(cron);
        if (startTime != null && startTime > System.currentTimeMillis() && preTriggerTime < startTime) {
            // 需要计算出离 startTime 最近的一次真正的触发时间
            Optional<ZonedDateTime> zonedDateTime = executionTime.lastExecution(ZonedDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.systemDefault()));
            preTriggerTime = zonedDateTime.map(dateTime -> dateTime.toEpochSecond() * 1000).orElse(startTime);
        }
        Instant instant = Instant.ofEpochMilli(preTriggerTime);
        ZonedDateTime preZonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        Optional<ZonedDateTime> opt = executionTime.nextExecution(preZonedDateTime);
        if (opt.isPresent()) {
            long nextTriggerTime = opt.get().toEpochSecond() * 1000;
            if (endTime != null && endTime < nextTriggerTime) {
                return null;
            }
            return nextTriggerTime;
        }
        return null;
    }

    @Override
    public TimeExpressionType supportType() {
        return TimeExpressionType.CRON;
    }
}
