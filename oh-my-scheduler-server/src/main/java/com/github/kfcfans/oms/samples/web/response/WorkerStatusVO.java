package com.github.kfcfans.oms.samples.web.response;

import com.github.kfcfans.oms.common.model.SystemMetrics;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.text.DecimalFormat;

/**
 * Worker机器状态
 *
 * @author tjq
 * @since 2020/4/14
 */
@Data
@NoArgsConstructor
public class WorkerStatusVO {

    private String address;
    private String cpuLoad;
    private String memoryLoad;
    private String diskLoad;

    // 1 -> 健康，绿色，2 -> 一般，橙色，3 -> 糟糕，红色
    private int status;

    // 12.3%(4 cores)
    private static final String CPU_FORMAT = "%s%%(%d cores)";
    // 27.7%(2.9/8.0 GB)
    private static final String OTHER_FORMAT = "%s%%（%s/%s GB）";
    private static final DecimalFormat df = new DecimalFormat("#.#");

    private static final double threshold = 0.8;

    public WorkerStatusVO(String address, SystemMetrics systemMetrics) {
        this.address = address;

        String cpuL = df.format(systemMetrics.getCpuLoad() * 100);
        this.cpuLoad = String.format(CPU_FORMAT, cpuL, systemMetrics.getCpuProcessors());


        String menL = df.format(systemMetrics.getJvmMemoryUsage() * 100);
        String menUsed = df.format(systemMetrics.getJvmUsedMemory());
        String menMax = df.format(systemMetrics.getJvmMaxMemory());
        this.memoryLoad = String.format(OTHER_FORMAT, menL, menUsed, menMax);

        String diskL = df.format(systemMetrics.getDiskUsage() * 100);
        String diskUsed = df.format(systemMetrics.getDiskUsed());
        String diskMax = df.format(systemMetrics.getDiskTotal());
        this.diskLoad = String.format(OTHER_FORMAT, diskL, diskUsed, diskMax);


        if (systemMetrics.getCpuLoad() < threshold && systemMetrics.getDiskUsage() < threshold && systemMetrics.getJvmMemoryUsage() < threshold) {
            status = 1;
        }else if (systemMetrics.getCpuLoad() > threshold && systemMetrics.getDiskUsage() > threshold && systemMetrics.getJvmMemoryUsage() > threshold) {
            status =  3;
        }else {
            status = 2;
        }
    }
}
