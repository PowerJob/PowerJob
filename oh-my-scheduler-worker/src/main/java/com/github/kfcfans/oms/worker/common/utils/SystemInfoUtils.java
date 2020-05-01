package com.github.kfcfans.oms.worker.common.utils;

import com.github.kfcfans.oms.common.model.SystemMetrics;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

/**
 * 系统信息工具，用于采集监控指标
 *
 * @author tjq
 * @since 2020/3/25
 */
public class SystemInfoUtils {

    // JMX bean can be accessed externally and is meant for management tools like hyperic ( or even nagios ) - It would delegate to Runtime anyway.
    private static final Runtime runtime = Runtime.getRuntime();
    private static OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();

    public static SystemMetrics getSystemMetrics() {

        SystemMetrics metrics = new SystemMetrics();

        // CPU 信息
        metrics.setCpuProcessors(osMXBean.getAvailableProcessors());
        metrics.setCpuLoad(osMXBean.getSystemLoadAverage() / osMXBean.getAvailableProcessors());

        // JVM内存信息(maxMemory指JVM能从操作系统获取的最大内存，即-Xmx参数设置的值，totalMemory指JVM当前持久的总内存)
        metrics.setJvmMaxMemory(bytes2GB(runtime.maxMemory()));
        // 已使用内存：当前申请总量 - 当前空余量
        metrics.setJvmUsedMemory(bytes2GB(runtime.totalMemory() - runtime.freeMemory()));
        // 百分比，直接 * 100
        metrics.setJvmMemoryUsage(1.0 * metrics.getJvmUsedMemory() / runtime.maxMemory());

        // 磁盘信息
        long free = 0;
        long total = 0;
        File[] roots = File.listRoots();
        for (File file : roots) {
            free += file.getFreeSpace();
            total += file.getTotalSpace();
        }

        metrics.setDiskUsed(bytes2GB(total - free));
        metrics.setDiskTotal(bytes2GB(total));
        metrics.setDiskUsage(metrics.getDiskUsed() / metrics.getDiskTotal() * 1.0);

        // 在Worker完成分数计算，减小Server压力
        metrics.calculateScore();
        return metrics;
    }


    private static double bytes2GB(long bytes) {
        return bytes / 1024.0 / 1024 / 1024;
    }

}
