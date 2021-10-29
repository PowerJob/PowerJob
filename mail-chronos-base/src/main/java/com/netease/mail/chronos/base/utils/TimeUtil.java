package com.netease.mail.chronos.base.utils;

import com.netease.mail.chronos.base.enums.BaseStatusEnum;
import com.netease.mail.chronos.base.exception.BaseException;
import lombok.SneakyThrows;
import lombok.val;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * @author Echo009
 * @since 2021/9/23
 */
public class TimeUtil {


    private static final ThreadLocal<SimpleDateFormat> COMMON_DATE_FORMAT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

    private static final ThreadLocal<SimpleDateFormat> NUMBER_DATE_FORMAT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyyMMdd"));

    private TimeUtil() {

    }

    public static TimeZone getTimeZoneByZoneId(String zoneId) {
        val check = checkTimeZoneId(zoneId);
        if (!check) {
            throw new BaseException(BaseStatusEnum.ILLEGAL_ARGUMENT.getCode(), "无效的 Time Zone Id : " + zoneId);
        }
        return TimeZone.getTimeZone(ZoneId.of(zoneId));
    }


    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean checkTimeZoneId(String timeZoneId) {
        try {
            ZoneId.of(timeZoneId);
        } catch (Exception e) {
            return false;
        }
        return true;
    }



    /**
     * 截取日期部分
     *
     * @param date 日期
     * @return 日期（时分秒信息会被清空）
     */
    @SneakyThrows
    public static Date truncate(Date date) {
        if (date == null) {
            return null;
        }
        final SimpleDateFormat simpleDateFormat = COMMON_DATE_FORMAT.get();
        return simpleDateFormat.parse(simpleDateFormat.format(date));
    }
    /**
     * 获取距离指定日期 N 天的日期，N 可以为负数
     */
    public static Date obtainNextNDay(Date date,int n){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date == null ? new Date():date);
        calendar.add(Calendar.DATE,n);
        return calendar.getTime();
    }

    public static Date obtainCurrentDate(){
        return truncate(new Date());
    }

    public static String obtainCurrentDateString(){
        return formatDate(new Date());
    }


    public static String formatDate(Date date){
        if (date == null) {
            return null;
        }
        return COMMON_DATE_FORMAT.get().format(date);
    }

    public static Integer getDateNumber(Date date){
        if (date == null) {
            return 0;
        }
        return Integer.parseInt(NUMBER_DATE_FORMAT.get().format(date));
    }

    /**
     * parse，如果输入无效则返回 null
     * @param dateString yyyy-MM-dd
     * @return 日期
     */

    public static Date parseDate(String dateString){
        try {
            return COMMON_DATE_FORMAT.get().parse(dateString);
        }catch (Exception ignore){

        }
        return null;
    }


    public void clear(){
        COMMON_DATE_FORMAT.remove();
        NUMBER_DATE_FORMAT.remove();
    }

}
