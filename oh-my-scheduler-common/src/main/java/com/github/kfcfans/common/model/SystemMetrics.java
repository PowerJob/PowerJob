package com.github.kfcfans.common.model;

import com.sun.istack.internal.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 系统指标
 *
 * @author tjq
 * @since 2020/3/25
 */
@Data
public class SystemMetrics implements Serializable, Comparable<SystemMetrics> {

    // CPU核心数量
    private int cpuProcessors;
    // CPU负载
    private double cpuLoad;

    // 内存（单位 GB）
    private double jvmUsedMemory;
    private double jvmTotalMemory;
    private double jvmMaxMemory;

    // 磁盘（单位 GB）
    private double diskUsed;
    private double diskTotal;
    private double diskUsage;

    // 缓存分数
    private int score;

    @Override
    public int compareTo(SystemMetrics that) {
        return this.calculateScore() - that.calculateScore();
    }

    /**
     * 计算得分情况，内存 > CPU > 磁盘
     * 磁盘必须有1G以上的剩余空间
     */
    public int calculateScore() {

        if (score > 0) {
            return score;
        }

        double availableCPUCores = cpuProcessors * cpuLoad;
        double availableMemory = jvmMaxMemory - jvmUsedMemory;
        double availableDisk = diskTotal - diskUsage;

        // 最低运行标准，1G磁盘 & 0.5G内存 & 一个可用的CPU核心
        if (availableDisk < 1 || availableMemory < 0.5 || availableCPUCores < 1) {
            score = 1;
        } else {
            // 磁盘只需要满足最低标准即可
            score = (int) (availableMemory * 2 + availableCPUCores);
        }

        return score;
    }
}
