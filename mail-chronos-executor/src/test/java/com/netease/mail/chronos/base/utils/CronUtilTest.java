package com.netease.mail.chronos.base.utils;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * @author Echo009
 * @since 2021/9/28
 */
class CronUtilTest {

    @SneakyThrows
    @Test
    void test() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = simpleDateFormat.parse("2021-09-28 12:00:00");
        long nextTriggerTime = CronUtil.calculateNextTriggerTime("0 0 13/4 * * ? ", "Asia/Shanghai", date.getTime());
        System.out.println(new Date(nextTriggerTime));
    }

    @Test
    @SneakyThrows
    void iCalendarTest() {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date2 = simpleDateFormat.parse("2021-09-29 20:00:00");
        Date date1 = simpleDateFormat.parse("2021-09-30 13:20:00");

        System.out.println(date1.getTime());

        long nextTriggerTime = ICalendarRecurrenceRuleUtil.calculateNextTriggerTime("FREQ=MINUTELY;INTERVAL=1;COUNT=1", date2.getTime(), date1.getTime());
        System.out.println(nextTriggerTime);
        System.out.println(simpleDateFormat.format(nextTriggerTime));
    }

}