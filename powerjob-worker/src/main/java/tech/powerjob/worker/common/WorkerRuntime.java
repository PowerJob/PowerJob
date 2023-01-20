package tech.powerjob.worker.common;

import tech.powerjob.remote.framework.transporter.Transporter;
import tech.powerjob.worker.background.OmsLogHandler;
import tech.powerjob.worker.background.ServerDiscoveryService;
import tech.powerjob.worker.background.WorkerHealthReporter;
import tech.powerjob.worker.core.executor.ExecutorManager;
import tech.powerjob.worker.persistence.TaskPersistenceService;
import lombok.Data;

/**
 * store worker's runtime
 *
 * @author tjq
 * @since 2021/3/7
 */
@Data
public class WorkerRuntime {

    private Long appId;

    private String workerAddress;

    private PowerJobWorkerConfig workerConfig;

    private Transporter transporter;

    private WorkerHealthReporter healthReporter;

    private ExecutorManager executorManager;

    private OmsLogHandler omsLogHandler;

    private ServerDiscoveryService serverDiscoveryService;

    private TaskPersistenceService taskPersistenceService;
}
