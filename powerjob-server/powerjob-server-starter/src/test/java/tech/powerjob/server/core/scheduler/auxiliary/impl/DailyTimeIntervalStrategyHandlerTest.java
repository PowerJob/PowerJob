package tech.powerjob.server.core.scheduler.auxiliary.impl;

import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tech.powerjob.common.OmsConstant;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.serialize.JsonUtils;

import java.util.List;
import java.util.Set;

/**
 * DailyTimeIntervalStrategyHandler
 * @author 550w
 * @date 2027/02/15
 */
@Slf4j
class DailyTimeIntervalStrategyHandlerTest {

    private static final DailyTimeIntervalStrategyHandler HD = new DailyTimeIntervalStrategyHandler();

    @Test
    void calculateInRangeTime() {

        // 2023-02-11 22:46:34
        long ts = 1676126794874L;
        // 验证范围中
        Long nowTs = DailyTimeIntervalStrategyHandler.calculateInRangeTime(ts, simpleBuild("21:00:00", "23:00:00", null));
        assert nowTs.equals(ts);

        // 时间未到（当天 23：00：00）
        Long nextTs = DailyTimeIntervalStrategyHandler.calculateInRangeTime(ts, simpleBuild("23:00:00", "23:15:00", null));
        assert nextTs.equals(1676127600000L);

        // 时间超出（第二天 11:00:00）
        Long nextDayStartTs = DailyTimeIntervalStrategyHandler.calculateInRangeTime(ts, simpleBuild("11:00:00", "12:15:00", null));
        assert nextDayStartTs.equals(1676170800000L);

        // 星期不满足（2023-02-15 11:00:00）
        Long notTodayStartTs = DailyTimeIntervalStrategyHandler.calculateInRangeTime(ts, simpleBuild("11:00:00", "12:15:00", Sets.newHashSet(3)));
        assert notTodayStartTs.equals(1676430000000L);
    }

    @Test
    void testValid() throws Exception {
        // 合法数据
        HD.validate(JsonUtils.toJSONString(simpleBuild("12:33:33", "13:33:33", null)));
        HD.validate(JsonUtils.toJSONString(simpleBuild("12:33:33", "13:33:33", Sets.newHashSet(1, 2, 3))));

        // start > end
        Assertions.assertThrowsExactly(IllegalArgumentException.class, () -> {
            HD.validate(JsonUtils.toJSONString(simpleBuild("17:33:33", "13:33:33", null)));
        });

        // start is empty
        Assertions.assertThrowsExactly(PowerJobException.class, () -> {
            HD.validate(JsonUtils.toJSONString(simpleBuild(null, "13:33:33", null)));
        });
    }

    @Test
    void testCalculateNextTriggerTime() {
        long ts = 1676126794874L;
        String expression = JsonUtils.toJSONString(simpleBuild("12:33:33", "15:34:33", Sets.newHashSet(1, 4)));
        int i = 0;
        List<Long> ret = Lists.newArrayList();
        for (;; i++) {
            Long triggerTime = HD.calculateNextTriggerTime(ts, expression, 1276126794874L, 1676735920000L);
            if (triggerTime == null) {
                break;
            }
            ret.add(triggerTime);
            ts = triggerTime;
            log.info("[DailyTimeIntervalStrategyHandlerTest] [calculateNextTriggerTime] {}st ->ts={},date={}", i, triggerTime, DateFormatUtils.format(triggerTime, OmsConstant.TIME_PATTERN));
        }
        assert i == 8;
        assert ret.equals(JSONArray.parseArray("[1676262813000, 1676266413000, 1676270013000, 1676273613000, 1676522013000, 1676525613000, 1676529213000, 1676532813000]", Long.class));
    }

    private static DailyTimeIntervalStrategyHandler.DailyTimeIntervalExpress simpleBuild(String startTimeOfDay, String endTimeOfDay, Set<Integer> days) {
        DailyTimeIntervalStrategyHandler.DailyTimeIntervalExpress ep = new DailyTimeIntervalStrategyHandler.DailyTimeIntervalExpress();
        ep.setInterval(3600L);
        ep.setStartTimeOfDay(startTimeOfDay);
        ep.setEndTimeOfDay(endTimeOfDay);
        ep.setDaysOfWeek(days);
        return ep;
    }
}