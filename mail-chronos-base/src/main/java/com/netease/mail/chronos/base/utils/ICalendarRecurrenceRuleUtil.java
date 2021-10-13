package com.netease.mail.chronos.base.utils;

import com.netease.mail.chronos.base.enums.BaseStatusEnum;
import com.netease.mail.chronos.base.exception.BaseException;
import net.fortuna.ical4j.model.*;

import java.text.ParseException;

/**
 * @author Echo009
 * @since 2021/9/29
 * <p>
 * http://ical4j.github.io/
 */
public class ICalendarRecurrenceRuleUtil {

    private ICalendarRecurrenceRuleUtil() {

    }



    public static long calculateNextTriggerTime(String recurrenceRule, long seedTime, long startTime) {
        Recur recur = construct(recurrenceRule);
        net.fortuna.ical4j.model.Date nextDate = recur.getNextDate(new DateTime(seedTime), new DateTime(startTime));
        return nextDate == null ? 0L : nextDate.getTime();
    }


    public static Recur construct(String recurrenceRule){
        try {
            return new Recur(recurrenceRule);
        } catch (ParseException parseException) {
            throw new BaseException(BaseStatusEnum.ILLEGAL_ARGUMENT.getCode(), "无效的日历重复规则" + recurrenceRule);
        }
    }

}
