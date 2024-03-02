package tech.powerjob.server.remote.worker.selector.impl;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import tech.powerjob.common.enums.DispatchStrategy;
import tech.powerjob.common.utils.CollectionUtils;
import tech.powerjob.server.common.module.WorkerInfo;
import tech.powerjob.server.persistence.remote.model.InstanceInfoDO;
import tech.powerjob.server.persistence.remote.model.JobInfoDO;
import tech.powerjob.server.remote.worker.selector.TaskTrackerSelector;
import tech.powerjob.server.remote.worker.utils.SpecifyUtils;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 指定工作的主节点，大规模运算需要隔离主节点，以防止 worker 部署打断整体的任务执行
 *
 * @author tjq
 * @since 2024/2/24
 */
@Slf4j
@Component
public class SpecifyTaskTrackerSelector implements TaskTrackerSelector {
    @Override
    public DispatchStrategy strategy() {
        return DispatchStrategy.SPECIFY;
    }

    @Override
    public WorkerInfo select(JobInfoDO jobInfoDO, InstanceInfoDO instanceInfoDO, List<WorkerInfo> availableWorkers) {

        String dispatchStrategyConfig = jobInfoDO.getDispatchStrategyConfig();

        // 降级到随机
        if (StringUtils.isEmpty(dispatchStrategyConfig)) {
            log.warn("[SpecifyTaskTrackerSelector] job[id={}]'s dispatchStrategyConfig is empty, use random as bottom DispatchStrategy!", jobInfoDO.getId());
            return availableWorkers.get(ThreadLocalRandom.current().nextInt(availableWorkers.size()));
        }

        List<WorkerInfo> targetWorkers = Lists.newArrayList();
        availableWorkers.forEach(aw -> {
            boolean match = SpecifyUtils.match(aw, dispatchStrategyConfig);
            if (match) {
                targetWorkers.add(aw);
            }
        });

        if (CollectionUtils.isEmpty(targetWorkers)) {
            log.warn("[SpecifyTaskTrackerSelector] Unable to find available nodes based on conditions for job(id={},dispatchStrategyConfig={}), use random as bottom DispatchStrategy!", jobInfoDO.getId(), dispatchStrategyConfig);
            return availableWorkers.get(ThreadLocalRandom.current().nextInt(availableWorkers.size()));
        }

        // 如果有多个 worker 符合条件，最终还是随机选择出一个
        return targetWorkers.get(ThreadLocalRandom.current().nextInt(targetWorkers.size()));
    }
}
