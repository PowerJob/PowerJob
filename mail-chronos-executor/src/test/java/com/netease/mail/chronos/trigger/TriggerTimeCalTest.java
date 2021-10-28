package com.netease.mail.chronos.trigger;

import com.netease.mail.chronos.base.utils.ICalendarRecurrenceRuleUtil;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.parameter.Value;
import org.junit.jupiter.api.Test;

import static com.netease.mail.chronos.base.utils.ICalendarRecurrenceRuleUtil.construct;

/**
 * @author Echo009
 * @since 2021/10/26
 */
class TriggerTimeCalTest {


    @Test
    void testReferenceRule(){

        Recur recur = construct("FREQ=DAILY;COUNT=10");

        DateList dates = recur.getDates(new Date(1634173200000L), new Date(1634950800000L), new Date(1645213479746L), Value.DATE_TIME);


        for (Date date : dates) {
            System.out.println(date);
        }
    }


    @Test
    void testCalTriggerTimeByReferenceRule() {
        long nextTriggerTime = ICalendarRecurrenceRuleUtil.calculateNextTriggerTime("FREQ=DAILY;COUNT=10", 1634173200000L, 1634950800000L);
        System.out.println(nextTriggerTime);
    }

    @Test
    void maxTime(){

        System.out.println(new Date(Long.MAX_VALUE));

    }


}
