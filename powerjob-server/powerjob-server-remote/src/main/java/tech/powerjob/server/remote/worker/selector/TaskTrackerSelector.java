package tech.powerjob.server.remote.worker.selector;

import tech.powerjob.common.enums.DispatchStrategy;
import tech.powerjob.server.common.module.WorkerInfo;
import tech.powerjob.server.persistence.remote.model.InstanceInfoDO;
import tech.powerjob.server.persistence.remote.model.JobInfoDO;

import java.util.List;

/**
 * 主节点选择方式
 *
 * @author tjq
 * @since 2024/2/24
 */
public interface TaskTrackerSelector {

    /**
     * 支持的策略
     * @return 派发策略
     */
    DispatchStrategy strategy();

    /**
     * 选择主节点
     * @param jobInfoDO 任务信息
     * @param instanceInfoDO 任务实例
     * @param availableWorkers 可用 workers
     * @return 主节点 worker
     */
    WorkerInfo select(JobInfoDO jobInfoDO, InstanceInfoDO instanceInfoDO, List<WorkerInfo> availableWorkers);
}
