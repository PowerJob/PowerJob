package com.github.kfcfans.powerjob.common.model;

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

    private SystemMetrics systemMetrics;

    private List<DeployedContainerInfo> containerInfos;

    public void refresh(WorkerHeartbeat workerHeartbeat) {
        address = workerHeartbeat.getWorkerAddress();
        lastActiveTime = workerHeartbeat.getHeartbeatTime();
        protocol = workerHeartbeat.getProtocol();
        systemMetrics = workerHeartbeat.getSystemMetrics();
        containerInfos = workerHeartbeat.getContainerInfos();
    }
}
