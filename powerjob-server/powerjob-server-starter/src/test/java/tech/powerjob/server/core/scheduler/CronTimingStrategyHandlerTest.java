package tech.powerjob.server.core.scheduler;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tech.powerjob.common.OmsConstant;
import tech.powerjob.server.core.scheduler.auxiliary.impl.CronTimingStrategyHandler;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

/**
 * @author Echo009
 * @since 2022/3/29
 */
@Slf4j
public class CronTimingStrategyHandlerTest {

    private final CronTimingStrategyHandler cronTimingStrategyHandler = new CronTimingStrategyHandler();

    private static final List<String> CRON_LIST = Lists.list(
            "0 0 10,11,12 * * ?",
            "0 0/30 9-17 * * ?",
            "0 0 12 ? * WED",
            "0 0 12 * * ?",
            "0 15 11 ? * *",
            "0 15 11 * * ?",
            "0 15 11 * * ? *",
            "0 15 11 * * ? 2088",
            "0 * 14 * * ?",
            "0 0/5 14 * * ?",
            "0 0/5 14,18 * * ?",
            "0 0-5 14 * * ?",
            "0 10,44 14 ? 3 WED",
            "0 15 11 ? * MON-FRI",
            "0 15 11 15 * ?",
            "0 15 11 L * ?",
            "0 15 11 ? * 6L",
            "0 15 11 ? * 6L 2087-2088",
            "0 15 11 ? * 6L 2087-2088",
            "0 15 11 ? * 6#3",
            "0 0 0 1 1 ? 2088"
    );


    @SneakyThrows
    @Test
    public void compareToQuartzCron() {
        // 对比 quartz cron 结果
        for (String cron : CRON_LIST) {
            Long referenceTime = System.currentTimeMillis();
            int count = 0;
            while (referenceTime != null && count < 50) {
                CronExpression cronExpression = new CronExpression(cron);
                Date nextValidTimeAfter = cronExpression.getNextValidTimeAfter(new Date(referenceTime));
                Long quartzRes = nextValidTimeAfter == null ? null : nextValidTimeAfter.getTime();
                Long newRes = cronTimingStrategyHandler.calculateNextTriggerTime(referenceTime, cron, null, null);
                log.info("cron:'{}',reference time:{},quartz result:{},new result:{}", cron, referenceTime, quartzRes, newRes);
                referenceTime = newRes;
                count++;
                Assertions.assertEquals(newRes, quartzRes);
            }
        }
    }

    @Test
    public void test01() {
        // cron 的有效区间小于 lifecycle
        String cron = "0 15 11 * * ? 2088-2089";
        Long referenceTime = System.currentTimeMillis();
        LocalDateTime start = LocalDateTime.of(2088, 5, 1, 11, 15, 0);
        LocalDateTime end = LocalDateTime.of(2099, 5, 1, 11, 15, 0);
        Long nextTriggerTime = cronTimingStrategyHandler.calculateNextTriggerTime(referenceTime, cron, start.toEpochSecond(ZoneOffset.of("+8")) * 1000, end.toEpochSecond(ZoneOffset.of("+8")) * 1000);
        Assertions.assertEquals("2088-05-01 11:15:00",DateFormatUtils.format(nextTriggerTime, OmsConstant.TIME_PATTERN));
    }


    @Test
    public void test02() {
        // cron 的有效区间大于 lifecycle
        String cron = "0 15 11 * * ? 2077-2099";
        Long referenceTime = System.currentTimeMillis();
        LocalDateTime start = LocalDateTime.of(2088, 5, 1, 11, 15, 0);
        LocalDateTime end = LocalDateTime.of(2099, 5, 1, 11, 15, 0);
        Long nextTriggerTime = cronTimingStrategyHandler.calculateNextTriggerTime(referenceTime, cron, start.toEpochSecond(ZoneOffset.of("+8")) * 1000, end.toEpochSecond(ZoneOffset.of("+8")) * 1000);
        Assertions.assertEquals("2088-05-01 11:15:00",DateFormatUtils.format(nextTriggerTime, OmsConstant.TIME_PATTERN));
    }
}
