package com.github.kfcfans.powerjob.server.extension;

import com.github.kfcfans.powerjob.server.persistence.core.model.JobInfoDO;
import com.github.kfcfans.powerjob.server.remote.worker.cluster.WorkerInfo;

/**
 * filter worker by system metrics or other info
 *
 * @author tjq
 * @since 2021/2/16
 */
public interface WorkerFilter {

    /**
     *
     * @param workerInfo worker info, maybe you need to use your customized info in SystemMetrics#extra
     * @param jobInfoDO job info
     * @return true will remove the worker in process list
     */
    boolean filter(WorkerInfo workerInfo, JobInfoDO jobInfoDO);
}
