package tech.powerjob.server.remote.worker.filter;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import tech.powerjob.server.common.SJ;
import tech.powerjob.server.common.module.WorkerInfo;
import tech.powerjob.server.persistence.remote.model.JobInfoDO;

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

    @Override
    public boolean filter(WorkerInfo workerInfo, JobInfoDO jobInfo) {

        String designatedWorkers = jobInfo.getDesignatedWorkers();

        // no worker is specified, no filter of any
        if (StringUtils.isEmpty(designatedWorkers)) {
            return false;
        }

        Set<String> designatedWorkersSet = Sets.newHashSet(SJ.COMMA_SPLITTER.splitToList(designatedWorkers));

        for (String tagOrAddress : designatedWorkersSet) {
            if (tagOrAddress.equals(workerInfo.getTag()) || tagOrAddress.equals(workerInfo.getAddress())) {
                return false;
            }
        }

        return true;
    }

}
