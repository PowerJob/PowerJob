package tech.powerjob.server.core.scheduler.auxiliary.impl;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DailyTimeIntervalStrategyHandler
 * @author 550w
 * @date 2027/02/15
 */
@Slf4j
class DailyTimeIntervalStrategyHandlerTest {

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

    private static DailyTimeIntervalStrategyHandler.DailyTimeIntervalExpress simpleBuild(String startTimeOfDay, String endTimeOfDay, Set<Integer> days) {
        DailyTimeIntervalStrategyHandler.DailyTimeIntervalExpress ep = new DailyTimeIntervalStrategyHandler.DailyTimeIntervalExpress();
        ep.setInterval(30L);
        ep.setStartTimeOfDay(startTimeOfDay);
        ep.setEndTimeOfDay(endTimeOfDay);
        ep.setDaysOfWeek(days);
        return ep;
    }
}