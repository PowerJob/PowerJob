package tech.powerjob.server.core.scheduler.auxiliary.impl;

import com.google.common.collect.Sets;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Component;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.common.utils.CollectionUtils;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.server.common.utils.TimeUtils;
import tech.powerjob.server.core.scheduler.auxiliary.TimeOfDay;
import tech.powerjob.server.core.scheduler.auxiliary.TimingStrategyHandler;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * DailyTimeIntervalStrategyHandler
 * @author 550w
 * @date 2027/02/15
 */
@Component
public class DailyTimeIntervalStrategyHandler implements TimingStrategyHandler {

    /**
     * 使用中国星期！！！
     */
    private static final Set<Integer> ALL_DAY = Sets.newHashSet(1, 2, 3, 4, 5, 6, 7);

    @Override
    public TimeExpressionType supportType() {
        return TimeExpressionType.DAILY_TIME_INTERVAL;
    }

    @Override
    @SneakyThrows
    public void validate(String timeExpression) {
        DailyTimeIntervalExpress ep = JsonUtils.parseObject(timeExpression, DailyTimeIntervalExpress.class);
        CommonUtils.requireNonNull(ep.interval, "interval can't be null or empty in DailyTimeIntervalExpress");
        CommonUtils.requireNonNull(ep.startTimeOfDay, "startTimeOfDay can't be null or empty in DailyTimeIntervalExpress");
        CommonUtils.requireNonNull(ep.endTimeOfDay, "endTimeOfDay can't be null or empty in DailyTimeIntervalExpress");

        TimeOfDay startTime = TimeOfDay.from(ep.startTimeOfDay);
        TimeOfDay endTime = TimeOfDay.from(ep.endTimeOfDay);

        if (endTime.before(startTime)) {
            throw new IllegalArgumentException("endTime should after startTime!");
        }

        if (StringUtils.isNotEmpty(ep.intervalUnit)) {
            TimeUnit.valueOf(ep.intervalUnit);
        }
    }

    @Override
    @SneakyThrows
    public Long calculateNextTriggerTime(Long preTriggerTime, String timeExpression, Long startTime, Long endTime) {
        DailyTimeIntervalExpress ep = JsonUtils.parseObject(timeExpression, DailyTimeIntervalExpress.class);

        // 未开始状态下，用起点算调度时间
        if (startTime != null && startTime > System.currentTimeMillis() && preTriggerTime < startTime) {
            return calculateInRangeTime(startTime, ep);
        }

        // 间隔时间
        TimeUnit timeUnit = Optional.ofNullable(ep.intervalUnit).map(TimeUnit::valueOf).orElse(TimeUnit.SECONDS);
        long interval = timeUnit.toMillis(ep.interval);

        Long ret = calculateInRangeTime(preTriggerTime + interval, ep);
        if (ret == null || ret <= Optional.ofNullable(endTime).orElse(Long.MAX_VALUE)) {
            return ret;
        }
        return null;
    }

    /**
     * 计算最近一次在范围中的时间
     * @param time 当前时间基准，可能直接返回该时间作为结果
     * @param ep 表达式
     * @return 最近一次在范围中的时间
     */
    static Long calculateInRangeTime(Long time, DailyTimeIntervalExpress ep) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(time));

        int year = calendar.get(Calendar.YEAR);
        // 月份 + 1，转为熟悉的 1～12 月
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // 判断是否符合"日"的执行条件
        int week = TimeUtils.calculateWeek(year, month, day);
        Set<Integer> targetDays = CollectionUtils.isEmpty(ep.daysOfWeek) ? ALL_DAY : ep.daysOfWeek;
        // 未包含情况下，将时间改写为符合条件日的 00:00 分，重新开始递归（这部分应该有性能更优的写法，不过这个调度模式应该很难触发瓶颈，先简单好用的实现）
        if (!targetDays.contains(week)) {
            simpleSetCalendar(calendar, 0, 0, 0);
            Date tomorrowZero = DateUtils.addDays(calendar.getTime(), 1);
            return calculateInRangeTime(tomorrowZero.getTime(), ep);
        }

        // 范围的开始时间
        TimeOfDay rangeStartTime = TimeOfDay.from(ep.startTimeOfDay);
        simpleSetCalendar(calendar, rangeStartTime.getHour(), rangeStartTime.getMinute(), rangeStartTime.getSecond());
        long todayStartTs = calendar.getTimeInMillis();

        // 未开始
        if (time < todayStartTs) {
            return todayStartTs;
        }

        TimeOfDay rangeEndTime = TimeOfDay.from(ep.endTimeOfDay);
        simpleSetCalendar(calendar, rangeEndTime.getHour(), rangeEndTime.getMinute(), rangeEndTime.getSecond());
        long todayEndTs = calendar.getTimeInMillis();

        // 范围之间
        if (time <= todayEndTs) {
            return time;
        }

        // 已结束，重新计算第二天时间
        simpleSetCalendar(calendar, 0, 0, 0);
        return calculateInRangeTime(DateUtils.addDays(calendar.getTime(), 1).getTime(), ep);
    }

    private static void simpleSetCalendar(Calendar calendar, int h, int m, int s) {
        calendar.set(Calendar.SECOND, s);
        calendar.set(Calendar.MINUTE, m);
        calendar.set(Calendar.HOUR_OF_DAY, h);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    @Data
    static class DailyTimeIntervalExpress implements Serializable {

        /**
         * 时间间隔
         */
        private Long interval;
        /**
         * 每天激活的时间起点，格式为：18:30:00 代表 18点30分00秒激活
         */
        private String startTimeOfDay;
        /**
         * 每日激活的时间终点，格式同上
         */
        private String endTimeOfDay;

        /* ************ 非必填字段 ************ */
        /**
         * 时间单位，默认秒
         */
        private String intervalUnit;
        /**
         * 每周的哪几天激活，空代表每天都激活
         */
        private Set<Integer> daysOfWeek;
    }
}
