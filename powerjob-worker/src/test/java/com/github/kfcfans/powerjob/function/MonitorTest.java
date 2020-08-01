package com.github.kfcfans.powerjob.function;

import com.github.kfcfans.powerjob.common.model.SystemMetrics;
import com.github.kfcfans.powerjob.common.utils.JsonUtils;
import com.github.kfcfans.powerjob.worker.common.utils.SystemInfoUtils;
import com.google.common.base.Stopwatch;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;

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
}
