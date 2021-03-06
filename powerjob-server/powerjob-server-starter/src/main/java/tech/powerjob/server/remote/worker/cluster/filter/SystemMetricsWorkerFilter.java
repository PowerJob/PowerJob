package tech.powerjob.server.remote.worker.cluster.filter;

import com.github.kfcfans.powerjob.common.model.SystemMetrics;
import tech.powerjob.server.extension.WorkerFilter;
import tech.powerjob.server.persistence.core.model.JobInfoDO;
import tech.powerjob.server.remote.worker.cluster.WorkerInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * filter worker by system metric
 *
 * @author tjq
 * @since 2021/2/19
 */
@Slf4j
@Component
public class SystemMetricsWorkerFilter implements WorkerFilter {

    @Override
    public boolean filter(WorkerInfo workerInfo, JobInfoDO jobInfo) {
        SystemMetrics metrics = workerInfo.getSystemMetrics();
        boolean filter = !metrics.available(jobInfo.getMinCpuCores(), jobInfo.getMinMemorySpace(), jobInfo.getMinDiskSpace());
        if (filter) {
            log.info("[Job-{}] filter worker[{}] because the {} do not meet the requirements", jobInfo.getId(), workerInfo.getAddress(), workerInfo.getSystemMetrics());
        }
        return filter;
    }
}
