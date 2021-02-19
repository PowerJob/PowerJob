package com.github.kfcfans.powerjob.server.remote.worker.cluster.filter;

import com.github.kfcfans.powerjob.server.common.SJ;
import com.github.kfcfans.powerjob.server.extension.WorkerFilter;
import com.github.kfcfans.powerjob.server.persistence.core.model.JobInfoDO;
import com.github.kfcfans.powerjob.server.remote.worker.cluster.WorkerInfo;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * just use designated worker
 *
 * @author tjq
 * @since 2021/2/19
 */
@Component
public class DesignatedWorkerFilter implements WorkerFilter {

    // min length 1.1.1.1:1
    private static final int MIN_IP_LENGTH = 9;

    @Override
    public boolean filter(WorkerInfo workerInfo, JobInfoDO jobInfoDO) {

        String designatedWorkers = jobInfoDO.getDesignatedWorkers();

        if (StringUtils.isEmpty(designatedWorkers) || designatedWorkers.length() < MIN_IP_LENGTH) {
            return false;
        }

        Set<String> designatedWorkersSet = Sets.newHashSet(SJ.commaSplitter.splitToList(designatedWorkers));
        return !designatedWorkersSet.contains(workerInfo.getAddress());
    }
}
