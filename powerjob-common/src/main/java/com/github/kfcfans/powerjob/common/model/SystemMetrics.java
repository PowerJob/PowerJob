package com.github.kfcfans.powerjob.common.model;

import com.github.kfcfans.powerjob.common.OmsSerializable;
import lombok.Data;

/**
 * 系统指标
 *
 * @author tjq
 * @since 2020/3/25
 */
@Data
public class SystemMetrics implements OmsSerializable, Comparable<SystemMetrics> {

    // CPU核心数量
    private int cpuProcessors;
    // CPU负载（负载 和 使用率 是两个完全不同的概念，Java 无法获取 CPU 使用率，只能获取负载）
    private double cpuLoad;

    // 内存（单位 GB）
    private double jvmUsedMemory;
    private double jvmMaxMemory;
    // 内存占用（0.X，非百分比）
    private double jvmMemoryUsage;

    // 磁盘（单位 GB）
    private double diskUsed;
    private double diskTotal;
    // 磁盘占用（0.X，非百分比）
    private double diskUsage;

    // 缓存分数
    private int score;

    @Override
    public int compareTo(SystemMetrics that) {
        // 降序排列
        return that.calculateScore() - this.calculateScore();
    }

    /**
     * 计算得分情况，内存 & CPU (磁盘不参与计算)
     * @return 得分情况
     */
    public int calculateScore() {

        if (score > 0) {
            return score;
        }

        // 对于 TaskTracker 来说，内存是任务顺利完成的关键，因此内存 2 块钱 1GB
        double memScore = (jvmMaxMemory - jvmUsedMemory) * 2;
        // CPU 剩余负载，1 块钱 1 斤
        double cpuScore = cpuProcessors - cpuLoad;
        // Indian Windows 无法获取 CpuLoad，为 -1，固定为 1
        if (cpuScore > cpuProcessors) {
            cpuScore = 1;
        }

        score = (int) (memScore + cpuScore);
        return score;
    }

    /**
     * 该机器是否可用
     * @param minCPUCores 判断标准之最低可用CPU核心数量
     * @param minMemorySpace 判断标准之最低可用内存
     * @param minDiskSpace 判断标准之最低可用磁盘空间
     * @return 是否可用
     */
    public boolean available(double minCPUCores, double minMemorySpace, double minDiskSpace) {

        double availableMemory = jvmMaxMemory - jvmUsedMemory;
        double availableDisk = diskTotal - diskUsed;

        if (availableMemory < minMemorySpace || availableDisk < minDiskSpace) {
            return false;
        }

        // cpuLoad 为负数代表无法获取，不判断。等于 0 为最理想情况，CPU 空载，不需要判断
        if (cpuLoad <= 0 || minCPUCores <= 0) {
            return true;
        }
        return minCPUCores < (cpuProcessors - cpuLoad);
    }
}
