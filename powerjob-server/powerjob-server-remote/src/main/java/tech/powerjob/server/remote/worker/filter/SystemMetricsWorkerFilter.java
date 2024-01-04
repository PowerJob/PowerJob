package tech.powerjob.server.remote.worker.filter;

import tech.powerjob.common.model.SystemMetrics;
import tech.powerjob.server.persistence.remote.model.JobInfoDO;
import tech.powerjob.server.common.module.WorkerInfo;
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
