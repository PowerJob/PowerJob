package tech.powerjob.server.core.scheduler;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.server.core.scheduler.auxiliary.TimingStrategyHandler;
import tech.powerjob.server.core.scheduler.auxiliary.impl.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Echo009
 * @since 2022/3/29
 */
public class TimingStrategyServiceTest {

    private final TimingStrategyService timingStrategyService;

    public TimingStrategyServiceTest() {
        List<TimingStrategyHandler> timingStrategyHandlers = new ArrayList<>();
        timingStrategyHandlers.add(new CronTimingStrategyHandler());
        timingStrategyHandlers.add(new ApiTimingStrategyHandler());
        timingStrategyHandlers.add(new FixedDelayTimingStrategyHandler());
        timingStrategyHandlers.add(new FixedRateTimingStrategyHandler());
        timingStrategyHandlers.add(new WorkflowTimingStrategyHandler());
        timingStrategyService = new TimingStrategyService(timingStrategyHandlers);
    }


    @Test
    public void testApiAndWorkflow() {
        // api
        Assertions.assertDoesNotThrow(() -> timingStrategyService.validate(TimeExpressionType.API, "", null, null));
        List<String> triggerTimes = timingStrategyService.calculateNextTriggerTimes(TimeExpressionType.API, "", null, null);
        Assertions.assertEquals(1, triggerTimes.size());
        // workflow
        Assertions.assertDoesNotThrow(() -> timingStrategyService.validate(TimeExpressionType.WORKFLOW, "", null, null));
        triggerTimes = timingStrategyService.calculateNextTriggerTimes(TimeExpressionType.WORKFLOW, "", null, null);
        Assertions.assertEquals(1, triggerTimes.size());
    }

    @Test
    public void testFixedRate() {
        // fixed rate
        Assertions.assertThrows(PowerJobException.class, () -> timingStrategyService.validate(TimeExpressionType.FIXED_RATE, "-0", null, null));
        Assertions.assertThrows(PowerJobException.class, () -> timingStrategyService.validate(TimeExpressionType.FIXED_RATE, "FFF", null, null));
        Assertions.assertThrows(PowerJobException.class, () -> timingStrategyService.validate(TimeExpressionType.FIXED_RATE, "300000", null, null));
        Assertions.assertDoesNotThrow(() -> timingStrategyService.validate(TimeExpressionType.FIXED_RATE, "10000", null, null));

        long timeParam = 1000;
        List<String> triggerTimes = timingStrategyService.calculateNextTriggerTimes(TimeExpressionType.FIXED_RATE, String.valueOf(timeParam), null, null);
        Assertions.assertEquals(5, triggerTimes.size());

        Long startTime = System.currentTimeMillis() + timeParam;
        Long endTime = System.currentTimeMillis() + timeParam * 3;
        triggerTimes = timingStrategyService.calculateNextTriggerTimes(TimeExpressionType.FIXED_RATE, String.valueOf(timeParam), startTime, endTime);
        Assertions.assertEquals(3, triggerTimes.size());

    }

    @Test
    public void testFixedDelay() {
        // fixed delay
        Assertions.assertThrows(PowerJobException.class, () -> timingStrategyService.validate(TimeExpressionType.FIXED_DELAY, "-0", null, null));
        Assertions.assertThrows(PowerJobException.class, () -> timingStrategyService.validate(TimeExpressionType.FIXED_DELAY, "FFF", null, null));
        Assertions.assertThrows(PowerJobException.class, () -> timingStrategyService.validate(TimeExpressionType.FIXED_DELAY, "300000", null, null));
        Assertions.assertDoesNotThrow(() -> timingStrategyService.validate(TimeExpressionType.FIXED_DELAY, "10000", null, null));

        List<String> triggerTimes = timingStrategyService.calculateNextTriggerTimes(TimeExpressionType.FIXED_DELAY, "1", null, null);
        Assertions.assertEquals(1, triggerTimes.size());
    }


    @Test
    public void testCron() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> timingStrategyService.validate(TimeExpressionType.CRON, "00 00 07 8-14,22-28 * 8", null, null));
        Assertions.assertDoesNotThrow(() -> timingStrategyService.validate(TimeExpressionType.CRON, "00 00 07 8-14,22-28 * 2", null, null));
        // https://github.com/PowerJob/PowerJob/issues/382
        // 支持同时指定 day-of-week 、day-of-month
        // 每隔一周的周一早上 7 点执行一次
        LocalDateTime start = LocalDateTime.of(2088, 5, 24, 7, 0, 0);
        LocalDateTime end = LocalDateTime.of(2088, 7, 12, 7, 0, 0);
        List<String> triggerTimes = timingStrategyService.calculateNextTriggerTimes(TimeExpressionType.CRON, "0 0 7 8-14,22-28 * 2", start.toEpochSecond(ZoneOffset.of("+8")) * 1000, end.toEpochSecond(ZoneOffset.of("+8")) * 1000);
        Assertions.assertNotNull(triggerTimes);
    }
}
