package tech.powerjob.server.remote.worker.selector.impl;

import org.springframework.stereotype.Component;
import tech.powerjob.common.enums.DispatchStrategy;
import tech.powerjob.server.common.module.WorkerInfo;
import tech.powerjob.server.persistence.remote.model.InstanceInfoDO;
import tech.powerjob.server.persistence.remote.model.JobInfoDO;
import tech.powerjob.server.remote.worker.selector.TaskTrackerSelector;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * RANDOM
 *
 * @author （疑似）新冠帕鲁
 * @since 2024/2/24
 */
@Component
public class RandomTaskTrackerSelector implements TaskTrackerSelector {

    @Override
    public DispatchStrategy strategy() {
        return DispatchStrategy.RANDOM;
    }

    @Override
    public WorkerInfo select(JobInfoDO jobInfoDO, InstanceInfoDO instanceInfoDO, List<WorkerInfo> availableWorkers) {
        int randomIdx = ThreadLocalRandom.current().nextInt(availableWorkers.size());
        return availableWorkers.get(randomIdx);
    }
}
