package com.netease.mail.chronos.base.utils;

import com.netease.mail.chronos.base.enums.BaseStatusEnum;
import com.netease.mail.chronos.base.exception.BaseException;
import lombok.val;

import java.time.ZoneId;
import java.util.TimeZone;

/**
 * @author Echo009
 * @since 2021/9/23
 */
public class TimeUtil {

    private TimeUtil() {

    }

    public static TimeZone getTimeZoneByZoneId(String zoneId) {
        val check = checkTimeZoneId(zoneId);
        if (!check) {
            throw new BaseException(BaseStatusEnum.ILLEGAL_ARGUMENT.getCode(), "无效的 Time Zone Id : " + zoneId);
        }
        return TimeZone.getTimeZone(ZoneId.of(zoneId));
    }


    public static boolean checkTimeZoneId(String timeZoneId) {
        try {
            val of = ZoneId.of(timeZoneId);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

}
