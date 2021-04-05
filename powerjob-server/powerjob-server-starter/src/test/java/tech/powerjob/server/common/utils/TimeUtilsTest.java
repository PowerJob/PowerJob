package tech.powerjob.server.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Date;

@Slf4j
class TimeUtilsTest {

    private static final String CRON = "0 0/5 * * * ? *";

    @Test
    void testEmptyOfLifecycle() throws Exception {
        Date date = TimeUtils.calculateNextCronTime(CRON, System.currentTimeMillis(), null);
        log.info("testEmptyOfLifecycle: {}", date);
        Assertions.assertNotNull(date);
    }

    @Test
    void testEndOfLifecycle() throws Exception {
        Date date = TimeUtils.calculateNextCronTime(CRON, System.currentTimeMillis(), "1217633201188-1417633201188");
        log.info("testEndOfLifecycle: {}", date);
        Assertions.assertNull(date);
    }

    @Test
    void testMultiLifecycle() throws Exception {
        String lifecycle = "1217633201188-1417633201188,1417633201188-1717633201188";
        Date date1 = TimeUtils.calculateNextCronTime(CRON, System.currentTimeMillis(), lifecycle);
        Date date2 = TimeUtils.calculateNextCronTime(CRON, System.currentTimeMillis(), null);

        log.info("testMultiLifecycle date1: {}", date1);
        log.info("testMultiLifecycle date2: {}", date2);

        Assertions.assertEquals(date1, date2);
    }

    @Test
    void check() {
        TimeUtils.checkServerTime();
    }
}