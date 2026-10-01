package tech.powerjob.server.core.scheduler.auxiliary.impl;

import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DailyTimeIntervalBoundaryTest {
    private final DailyTimeIntervalStrategyHandler handler = new DailyTimeIntervalStrategyHandler();

    private static String expression(long interval, String unit, String days) {
        return "{\"interval\":" + interval + ",\"intervalUnit\":\"" + unit
                + "\",\"startTimeOfDay\":\"18:00:00\",\"endTimeOfDay\":\"19:10:00\",\"daysOfWeek\":" + days + "}";
    }

    private static long at(String time) throws Exception {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(time).getTime();
    }

    @Test
    void startsAtWindowBoundaryInsteadOfAddingIntervalBeforeTheWindow() throws Exception {
        assertEquals(Long.valueOf(at("2026-09-30 18:00:00")), handler.calculateNextTriggerTime(
                at("2026-09-30 17:50:00"), expression(30, "MINUTES", "[]"), null, null));
    }

    @Test
    void preservesIntervalsInsideTheWindowAndIncludesTheEnd() throws Exception {
        String rule = expression(10, "MINUTES", "[]");
        assertEquals(Long.valueOf(at("2026-09-30 19:10:00")),
                handler.calculateNextTriggerTime(at("2026-09-30 19:00:00"), rule, null, null));
        assertEquals(Long.valueOf(at("2026-10-01 18:00:00")),
                handler.calculateNextTriggerTime(at("2026-09-30 19:10:00"), rule, null, null));
    }

    @Test
    void skipsExcludedDaysWithoutShiftingTheNextWindowStart() throws Exception {
        assertEquals(Long.valueOf(at("2026-10-05 18:00:00")), handler.calculateNextTriggerTime(
                at("2026-09-30 17:50:00"), expression(30, "MINUTES", "[1]"), null, null));
    }

    @Test
    void honorsAbsoluteLifecycleIncludingItsEndLimit() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 2);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long start = calendar.getTimeInMillis();
        calendar.set(Calendar.HOUR_OF_DAY, 18);
        long first = calendar.getTimeInMillis();
        String rule = expression(30, "MINUTES", "[]");
        assertEquals(Long.valueOf(first), handler.calculateNextTriggerTime(start - 1000, rule, start, first));
        assertNull(handler.calculateNextTriggerTime(start - 1000, rule, start, first - 1));
    }

    @Test
    void honorsHistoricalStartWithoutDependingOnTheWallClock() throws Exception {
        assertEquals(Long.valueOf(at("2000-01-02 18:00:00")), handler.calculateNextTriggerTime(
                at("2000-01-01 17:50:00"), expression(30, "MINUTES", "[]"), at("2000-01-02 12:00:00"), null));
    }

    @Test
    void rejectsNonPositiveIntervalsInvalidWeekdaysAndOverflow() {
        for (long interval : new long[] {0, -1, Long.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> handler.validate(expression(interval, "SECONDS", "[]")));
        }
        for (String days : new String[] {"[0]", "[8]", "[null]"}) {
            assertThrows(IllegalArgumentException.class, () -> handler.validate(expression(1, "SECONDS", days)));
            assertThrows(IllegalArgumentException.class, () -> handler.calculateNextTriggerTime(
                    1L, expression(1, "SECONDS", days), null, null));
        }
        assertThrows(IllegalArgumentException.class, () -> handler.validate(expression(1, "NANOSECONDS", "[]")));
    }
}
