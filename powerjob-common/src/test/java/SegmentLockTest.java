import tech.powerjob.common.utils.SegmentLock;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 分段锁测试
 *
 * @author tjq
 * @since 2020/6/15
 */
public class SegmentLockTest {

    @Test
    public void testLock() throws Exception {
        int lockId = 10086;
        SegmentLock lock = new SegmentLock(4);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        pool.execute(() -> {
            System.out.println("before lock A");
            lock.lockInterruptibleSafe(lockId);
            System.out.println("after lock A");
        });

        pool.execute(() -> {
            System.out.println("before lock AA");
            lock.lockInterruptibleSafe(lockId);
            System.out.println("after lock AA");
        });

        Thread.sleep(10000);
    }

    @Test
    public void testUnLock() throws Exception {
        int lockId = 10086;
        SegmentLock lock = new SegmentLock(4);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        pool.execute(() -> {
            System.out.println("before lock A");
            lock.lockInterruptibleSafe(lockId);
            System.out.println("after lock A");
            try {
                Thread.sleep(5000);
            }catch (Exception ignore) {
            }
            lock.unlock(lockId);
        });

        pool.execute(() -> {
            System.out.println("before lock AA");
            lock.lockInterruptibleSafe(lockId);
            System.out.println("after lock AA");
        });

        Thread.sleep(10000);
    }

}
