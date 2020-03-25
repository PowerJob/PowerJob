package com.github.kfcfans.common.model;

import lombok.Data;

/**
 * 系统指标
 *
 * @author tjq
 * @since 2020/3/25
 */
@Data
public class SystemMetrics {

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

}
