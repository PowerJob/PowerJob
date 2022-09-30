package tech.powerjob.server.web.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import tech.powerjob.common.model.SystemMetrics;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.server.common.module.WorkerInfo;

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

    private String protocol;
    private String tag;
    private String lastActiveTime;

    private Integer lightTaskTrackerNum;

    private Integer heavyTaskTrackerNum;

    private long lastOverloadTime;

    private boolean overloading;

    /**
     * 1 -> 健康，绿色，2 -> 一般，橙色，3 -> 糟糕，红色，9999 -> 非在线机器
     */
    private int status;

    /**
     *  12.3%(4 cores)
     */
    private static final String CPU_FORMAT = "%s / %s cores";
    /**
     *  27.7%(2.9/8.0 GB)
     */
    private static final String OTHER_FORMAT = "%s%%（%s / %s GB）";
    private static final DecimalFormat df = new DecimalFormat("#.#");

    private static final double THRESHOLD = 0.8;

    public WorkerStatusVO(WorkerInfo workerInfo) {

        SystemMetrics systemMetrics = workerInfo.getSystemMetrics();

        this.status = 1;
        this.address = workerInfo.getAddress();
        this.cpuLoad = String.format(CPU_FORMAT, df.format(systemMetrics.getCpuLoad()), systemMetrics.getCpuProcessors());
        if (systemMetrics.getCpuLoad() > systemMetrics.getCpuProcessors() * THRESHOLD) {
            this.status ++;
        }

        String menL = df.format(systemMetrics.getJvmMemoryUsage() * 100);
        String menUsed = df.format(systemMetrics.getJvmUsedMemory());
        String menMax = df.format(systemMetrics.getJvmMaxMemory());
        this.memoryLoad = String.format(OTHER_FORMAT, menL, menUsed, menMax);
        if (systemMetrics.getJvmMemoryUsage() > THRESHOLD) {
            this.status ++;
        }

        String diskL = df.format(systemMetrics.getDiskUsage() * 100);
        String diskUsed = df.format(systemMetrics.getDiskUsed());
        String diskMax = df.format(systemMetrics.getDiskTotal());
        this.diskLoad = String.format(OTHER_FORMAT, diskL, diskUsed, diskMax);
        if (systemMetrics.getDiskUsage() > THRESHOLD) {
            this.status ++;
        }

        if (workerInfo.overload()){
            // 超载的情况直接置为 3
            this.status = 3;
        }

        if (workerInfo.timeout()) {
            this.status = 9999;
        }

        this.protocol = workerInfo.getProtocol();
        this.tag = CommonUtils.formatString(workerInfo.getTag());
        this.lastActiveTime = CommonUtils.formatTime(workerInfo.getLastActiveTime());
        this.lightTaskTrackerNum = workerInfo.getLightTaskTrackerNum();
        this.heavyTaskTrackerNum = workerInfo.getHeavyTaskTrackerNum();
        this.lastOverloadTime = workerInfo.getLastOverloadTime();
        this.overloading = workerInfo.isOverloading();
    }
}
