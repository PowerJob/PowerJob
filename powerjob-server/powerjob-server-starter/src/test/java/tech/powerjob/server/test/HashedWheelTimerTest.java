package tech.powerjob.server.test;

import org.junit.jupiter.api.Test;
import tech.powerjob.server.common.timewheel.HashedWheelTimer;
import tech.powerjob.server.common.timewheel.TimerFuture;
import tech.powerjob.server.common.timewheel.TimerTask;
import tech.powerjob.server.common.timewheel.holder.InstanceTimeWheelService;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 时间轮测试
 *
 * @author tjq
 * @since 2020/11/28
 */
@Slf4j
public class HashedWheelTimerTest {

    @Test
    public void testHashedWheelTimer() throws Exception {

        HashedWheelTimer timer = new HashedWheelTimer(1, 1024, 32);
        List<TimerFuture> futures = Lists.newLinkedList();

        for (int i = 0; i < 10; i++) {

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

        Thread.sleep(10);

        // 关闭
        System.out.println(timer.stop().size());
        System.out.println("Finished！");

        Thread.sleep(27);
    }

    @Test
    public void testPerformance() throws Exception {
        Stopwatch sw = Stopwatch.createStarted();
        for (long i = 0; i < 10; i++) {
            long delay = ThreadLocalRandom.current().nextLong(100, 120000);
            long expect = System.currentTimeMillis() + delay;
            InstanceTimeWheelService.schedule(i, delay, () -> {
               log.info("[Performance] deviation:{}", (System.currentTimeMillis() - expect));
            });
        }
        log.info("[Performance] insert cost: {}", sw);

        Thread.sleep(90);
    }

    @Test
    public void testLongDelayTask() throws Exception {
        for (long i = 0; i < 10; i++) {
            long delay = ThreadLocalRandom.current().nextLong(60000, 60000 * 3);
            long expect = System.currentTimeMillis() + delay;
            InstanceTimeWheelService.schedule(i, delay, () -> {
                log.info("[LongDelayTask] deviation: {}", (System.currentTimeMillis() - expect));
            });
        }

        Thread.sleep(60 * 4);
    }

    @Test
    public void testCancelDelayTask() throws Exception {

        AtomicLong executeNum = new AtomicLong();
        AtomicLong cancelNum = new AtomicLong();
        for (long i = 0; i < 10; i++) {
            long delay = ThreadLocalRandom.current().nextLong(60000, 60000 * 2);
            long expect = System.currentTimeMillis() + delay;
            InstanceTimeWheelService.schedule(i, delay, () -> {
                executeNum.incrementAndGet();
                log.info("[CancelLongDelayTask] deviation: {}", (System.currentTimeMillis() - expect));
            });
        }

        Thread.sleep(10);

        for (long i = 0; i < 10; i++) {
            boolean nextBoolean = ThreadLocalRandom.current().nextBoolean();
            if (nextBoolean) {
                continue;
            }
            boolean cancel = InstanceTimeWheelService.fetchTimerFuture(i).cancel();
            log.info("[CancelLongDelayTask] id:{},status:{}", i, cancel);
            cancelNum.incrementAndGet();
        }

        Thread.sleep(60 * 4);
        log.info("[CancelLongDelayTask] result -> executeNum:{},cancelNum:{}", executeNum, cancelNum);
    }
}
