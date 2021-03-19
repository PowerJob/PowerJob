package tech.powerjob.worker.common;

import akka.actor.ActorSystem;
import tech.powerjob.worker.background.OmsLogHandler;
import tech.powerjob.worker.background.ServerDiscoveryService;
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

    private PowerJobWorkerConfig workerConfig;

    private String workerAddress;

    private ActorSystem actorSystem;
    private OmsLogHandler omsLogHandler;
    private ServerDiscoveryService serverDiscoveryService;
    private TaskPersistenceService taskPersistenceService;
}
