package com.github.kfcfans.powerjob.server.remote.worker.cluster.filter;

import com.github.kfcfans.powerjob.server.extension.WorkerFilter;
import com.github.kfcfans.powerjob.server.persistence.core.model.JobInfoDO;
import com.github.kfcfans.powerjob.server.remote.worker.cluster.WorkerInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * filter disconnected worker
 *
 * @author tjq
 * @since 2021/2/19
 */
@Slf4j
@Component
public class DisconnectedWorkerFilter implements WorkerFilter {

    @Override
    public boolean filter(WorkerInfo workerInfo, JobInfoDO jobInfo) {
        boolean timeout = workerInfo.timeout();
        if (timeout) {
            log.info("[Job-{}] filter worker[{}] due to timeout(lastActiveTime={})", jobInfo.getId(), workerInfo.getAddress(), workerInfo.getLastActiveTime());
        }
        return timeout;
    }
}
