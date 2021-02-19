package com.github.kfcfans.powerjob.server.remote.worker.cluster;

import com.github.kfcfans.powerjob.server.extension.WorkerFilter;
import com.github.kfcfans.powerjob.server.persistence.core.model.JobInfoDO;
import com.github.kfcfans.powerjob.server.remote.server.redirector.DesignateServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 获取 worker 集群信息
 *
 * @author tjq
 * @since 2021/2/19
 */
@Service
public class WorkerClusterQueryService {

    private List<WorkerFilter> workerFilters;

    @Autowired
    public WorkerClusterQueryService(List<WorkerFilter> workerFilters) {
        this.workerFilters = workerFilters;
    }

    /**
     * get worker for job
     * @param jobInfo job
     * @return worker cluster info, sorted by metrics desc
     */
    public List<WorkerInfo> getSuitableWorkers(JobInfoDO jobInfo) {

        List<WorkerInfo> workers = WorkerClusterManagerService.getWorkerInfosByAppId(jobInfo.getAppId());

        workers.removeIf(workerInfo -> filterWorker(workerInfo, jobInfo));

        // 按健康度排序
        workers.sort((o1, o2) -> o2 .getSystemMetrics().calculateScore() - o1.getSystemMetrics().calculateScore());

        // 限定集群大小（0代表不限制）
        if (!workers.isEmpty() && jobInfo.getMaxWorkerCount() > 0 && workers.size() > jobInfo.getMaxWorkerCount()) {
            workers = workers.subList(0, jobInfo.getMaxWorkerCount());
        }
        return workers;
    }

    @DesignateServer(appIdParameterName = "appId")
    public List<WorkerInfo> getAllWorkers(Long appId) {
        List<WorkerInfo> workers = WorkerClusterManagerService.getWorkerInfosByAppId(appId);
        workers.sort((o1, o2) -> o2 .getSystemMetrics().calculateScore() - o1.getSystemMetrics().calculateScore());
        return workers;
    }

    private boolean filterWorker(WorkerInfo workerInfo, JobInfoDO jobInfo) {
        for (WorkerFilter filter : workerFilters) {
            if (filter.filter(workerInfo, jobInfo)) {
                return true;
            }
        }
        return false;
    }
}
