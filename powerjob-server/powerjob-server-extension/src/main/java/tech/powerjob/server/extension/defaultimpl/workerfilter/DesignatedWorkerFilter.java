package tech.powerjob.server.extension.defaultimpl.workerfilter;

import tech.powerjob.server.common.SJ;
import tech.powerjob.server.extension.WorkerFilter;
import tech.powerjob.server.persistence.core.model.JobInfoDO;
import tech.powerjob.server.common.module.WorkerInfo;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * just use designated worker
 *
 * @author tjq
 * @since 2021/2/19
 */
@Slf4j
@Component
public class DesignatedWorkerFilter implements WorkerFilter {

    // min length 1.1.1.1:1
    private static final int MIN_ADDRESS_LENGTH = 9;

    @Override
    public boolean filter(WorkerInfo workerInfo, JobInfoDO jobInfo) {

        String designatedWorkers = jobInfo.getDesignatedWorkers();

        if (StringUtils.isEmpty(designatedWorkers) || designatedWorkers.length() < MIN_ADDRESS_LENGTH) {
            return false;
        }

        Set<String> designatedWorkersSet = Sets.newHashSet(SJ.COMMA_SPLITTER.splitToList(designatedWorkers));

        return !designatedWorkersSet.contains(workerInfo.getAddress());
    }
}
