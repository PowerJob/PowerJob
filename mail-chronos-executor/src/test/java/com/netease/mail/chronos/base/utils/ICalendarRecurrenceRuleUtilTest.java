package com.netease.mail.chronos.base.utils;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Echo009
 * @since 2021/10/13
 */
class ICalendarRecurrenceRuleUtilTest {

    @Test
    @SneakyThrows
    void calculateNextTriggerTime() {


        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date2 = simpleDateFormat.parse("2021-09-29 20:00:00");
        Date date1 = simpleDateFormat.parse("2021-09-30 01:00:00");


        long nextTriggerTime = ICalendarRecurrenceRuleUtil.calculateNextTriggerTime("FREQ=MINUTELY;INTERVAL=60;COUNT=24;UNTIL=20210930T010000Z", date2.getTime(), date1.getTime());
        System.out.println(nextTriggerTime);
        System.out.println(simpleDateFormat.format(nextTriggerTime));

    }
}