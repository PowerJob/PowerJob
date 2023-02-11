package tech.powerjob.server.core.scheduler.auxiliary.impl;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.time.DateUtils;
import tech.powerjob.common.OmsConstant;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.server.core.scheduler.auxiliary.TimeOfDay;
import tech.powerjob.server.core.scheduler.auxiliary.TimingStrategyHandler;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

/**
 * DailyTimeIntervalStrategyHandler
 * @author 550w
 * @date 2027/02/15
 */
public class DailyTimeIntervalStrategyHandler implements TimingStrategyHandler {

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
    }

    @Override
    public Long calculateNextTriggerTime(Long preTriggerTime, String timeExpression, Long startTime, Long endTime) {

        return null;
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
