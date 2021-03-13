package com.github.kfcfans.powerjob.worker.common;

import akka.actor.ActorSystem;
import com.github.kfcfans.powerjob.worker.background.OmsLogHandler;
import com.github.kfcfans.powerjob.worker.background.ServerDiscoveryService;
import com.github.kfcfans.powerjob.worker.persistence.TaskPersistenceService;
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

    private OhMyConfig ohMyConfig;

    private String workerAddress;

    private ActorSystem actorSystem;
    private OmsLogHandler omsLogHandler;
    private ServerDiscoveryService serverDiscoveryService;
    private TaskPersistenceService taskPersistenceService;
}
