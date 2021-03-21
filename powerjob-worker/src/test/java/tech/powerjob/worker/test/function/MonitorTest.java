package tech.powerjob.worker.test.function;

import tech.powerjob.common.model.SystemMetrics;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.worker.common.utils.SystemInfoUtils;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.Collections;
import java.util.List;

/**
 * 测试监控指标
 *
 * @author tjq
 * @since 2020/8/1
 */
public class MonitorTest {

    private static final OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
    private static final Runtime runtime = Runtime.getRuntime();

    @Test
    public void testGetSystemLoadAverage() {
        for (int i = 0; i < 10000; i++) {
            double average = osMXBean.getSystemLoadAverage();
            System.out.println(average);
            System.out.println(average / osMXBean.getAvailableProcessors());
            try {
                Thread.sleep(1000);
            }catch (Exception ignore) {
            }
        }
    }

    @Test
    public void testListDisk() {
        Stopwatch sw = Stopwatch.createStarted();
        SystemMetrics systemMetrics = SystemInfoUtils.getSystemMetrics();
        System.out.println(JsonUtils.toJSONString(systemMetrics));
        System.out.println(sw.stop());
        Stopwatch sw2 = Stopwatch.createStarted();
        System.out.println(systemMetrics.calculateScore());
        System.out.println(sw2.stop());
    }

    @Test
    public void testMemory() {
        System.out.println("- used:" + (runtime.totalMemory() - runtime.freeMemory()));
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        System.out.println("heap used: " + memoryMXBean.getHeapMemoryUsage());
        System.out.println("noheap used: " + memoryMXBean.getNonHeapMemoryUsage());
    }

    @Test
    public void testFetchMetrics() {
        SystemMetrics systemMetrics = SystemInfoUtils.getSystemMetrics();
        System.out.println(JsonUtils.toJSONString(systemMetrics));
    }

    @Test
    public void testSortMetrics() {
        SystemMetrics high = new SystemMetrics();
        high.setScore(100);
        SystemMetrics low = new SystemMetrics();
        low.setScore(1);
        List<SystemMetrics> list = Lists.newArrayList(high, low);
        list.sort((o1, o2) -> o2.calculateScore() - o1.calculateScore());
        System.out.println(list);

        Collections.sort(list);
        System.out.println(list);
    }
}
