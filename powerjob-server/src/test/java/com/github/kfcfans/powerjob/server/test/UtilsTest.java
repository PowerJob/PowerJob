package com.github.kfcfans.powerjob.server.test;

import com.github.kfcfans.powerjob.server.OhMyApplication;
import com.github.kfcfans.powerjob.server.common.utils.CronExpression;
import com.github.kfcfans.powerjob.server.common.utils.timewheel.HashedWheelTimer;
import com.github.kfcfans.powerjob.server.common.utils.timewheel.TimerFuture;
import com.github.kfcfans.powerjob.server.common.utils.timewheel.TimerTask;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 工具类测试
 *
 * @author tjq
 * @since 2020/4/3
 */
public class UtilsTest {

    @Test
    public void testHashedWheelTimer() throws Exception {

        HashedWheelTimer timer = new HashedWheelTimer(1, 1024, 32);
        List<TimerFuture> futures = Lists.newLinkedList();

        for (int i = 0; i < 1000; i++) {

            String name = "Task" + i;
            long nowMS = System.currentTimeMillis();
            int delayMS = ThreadLocalRandom.current().nextInt(60000);
            long targetTime = delayMS + nowMS;

            TimerTask timerTask = () -> {
                System.out.println("============= " + name + "============= ");
                System.out.println("ThreadInfo:" + Thread.currentThread().getName());
                System.out.println("expectTime:" + targetTime);;
                System.out.println("currentTime:" + System.currentTimeMillis());
                System.out.println("deviation:" + (System.currentTimeMillis() - targetTime));
                System.out.println("============= " + name + "============= ");
            };
            futures.add(timer.schedule(timerTask, delayMS, TimeUnit.MILLISECONDS));
        }

        // 随机取消
        futures.forEach(future -> {

            int x = ThreadLocalRandom.current().nextInt(2);
            if (x == 1) {
                future.cancel();
            }

        });

        Thread.sleep(1000);

        // 关闭
        System.out.println(timer.stop().size());
        System.out.println("Finished！");

        Thread.sleep(277777777);
    }

    @Test
    public void testCronExpression() throws Exception {
        String cron = "0 * * * * ? *";
        CronExpression cronExpression = new CronExpression(cron);
        final Date nextValidTimeAfter = cronExpression.getNextValidTimeAfter(new Date());
        System.out.println(nextValidTimeAfter);
    }

    @Test
    public void normalTest() {
        String s = "000000000111010000001100000000010110100110100000000001000000000000";
        System.out.println(s.length());
    }

    @Test
    public void testTZ() {
        System.out.println(TimeZone.getDefault());
    }

    @Test
    public void testStringUtils() {
        String goodAppName = "powerjob-server";
        String appName = "powerjob-server ";
        System.out.println(StringUtils.containsWhitespace(goodAppName));
        System.out.println(StringUtils.containsWhitespace(appName));
    }

    @Test
    public void testPre() {
        OhMyApplication.pre();
    }
}
