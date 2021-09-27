package com.netease.mail.chronos.base.utils;

import com.netease.mail.chronos.base.cron.CronExpression;
import com.netease.mail.chronos.base.enums.BaseStatusEnum;
import com.netease.mail.chronos.base.exception.BaseException;

import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;

/**
 * @author Echo009
 * @since 2021/9/27
 */
public class CronUtil {


    private CronUtil() {

    }

    /**
     * 计算下一次调度时间
     *
     * @param cron       cron 表达式
     * @param timeZoneId tzId
     * @param startTime  开始时间
     * @return 下次调度时间
     */
    public static long calculateNextTriggerTime(String cron, String timeZoneId, long startTime) {
        TimeZone tz = TimeUtil.getTimeZoneByZoneId(timeZoneId);
        CronExpression cronExpression;
        try {
            cronExpression = new CronExpression(cron);

        } catch (ParseException e) {
            throw new BaseException(BaseStatusEnum.ILLEGAL_ARGUMENT.getCode(), "无效的 cron 表达式" + cron);
        }
        cronExpression.setTimeZone(tz);
        return cronExpression.getNextValidTimeAfter(new Date(startTime)).getTime();

    }


}
