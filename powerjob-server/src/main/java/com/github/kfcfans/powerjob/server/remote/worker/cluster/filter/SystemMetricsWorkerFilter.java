package com.github.kfcfans.powerjob.server.remote.worker.cluster.filter;

import com.github.kfcfans.powerjob.common.model.SystemMetrics;
import com.github.kfcfans.powerjob.server.extension.WorkerFilter;
import com.github.kfcfans.powerjob.server.persistence.core.model.JobInfoDO;
import com.github.kfcfans.powerjob.server.remote.worker.cluster.WorkerInfo;
import org.springframework.stereotype.Component;

/**
 * filter worker by system metric
 *
 * @author tjq
 * @since 2021/2/19
 */
@Component
public class SystemMetricsWorkerFilter implements WorkerFilter {

    @Override
    public boolean filter(WorkerInfo workerInfo, JobInfoDO jobInfo) {
        SystemMetrics metrics = workerInfo.getSystemMetrics();
        return !metrics.available(jobInfo.getMinCpuCores(), jobInfo.getMinMemorySpace(), jobInfo.getMinDiskSpace());
    }
}
