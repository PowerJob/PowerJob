package com.github.kfcfans.powerjob.server.remote.worker.cluster;

import com.github.kfcfans.powerjob.common.model.DeployedContainerInfo;
import com.github.kfcfans.powerjob.common.model.SystemMetrics;
import com.github.kfcfans.powerjob.common.request.WorkerHeartbeat;
import lombok.Data;

import java.util.List;

/**
 * worker info
 *
 * @author tjq
 * @since 2021/2/7
 */
@Data
public class WorkerInfo {

    private String address;

    private long lastActiveTime;

    private String protocol;

    private String client;

    private SystemMetrics systemMetrics;

    private List<DeployedContainerInfo> containerInfos;

    private static final long WORKER_TIMEOUT_MS = 60000;

    public void refresh(WorkerHeartbeat workerHeartbeat) {
        address = workerHeartbeat.getWorkerAddress();
        lastActiveTime = workerHeartbeat.getHeartbeatTime();
        protocol = workerHeartbeat.getProtocol();
        client = workerHeartbeat.getClient();
        systemMetrics = workerHeartbeat.getSystemMetrics();
        containerInfos = workerHeartbeat.getContainerInfos();
    }

    public boolean timeout() {
        long timeout = System.currentTimeMillis() - lastActiveTime;
        return timeout > WORKER_TIMEOUT_MS;
    }
}
