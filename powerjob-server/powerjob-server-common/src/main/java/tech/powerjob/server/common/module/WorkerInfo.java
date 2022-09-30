package tech.powerjob.server.common.module;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import tech.powerjob.common.model.DeployedContainerInfo;
import tech.powerjob.common.model.SystemMetrics;
import tech.powerjob.common.request.WorkerHeartbeat;

import java.util.List;

/**
 * worker info
 *
 * @author tjq
 * @since 2021/2/7
 */
@Data
@Slf4j
public class WorkerInfo {

    private String address;

    private long lastActiveTime;

    private String protocol;

    private String client;

    private String tag;

    private int lightTaskTrackerNum;

    private int heavyTaskTrackerNum;

    private long lastOverloadTime;

    private boolean overloading;

    private SystemMetrics systemMetrics;

    private List<DeployedContainerInfo> containerInfos;

    private static final long WORKER_TIMEOUT_MS = 60000;

    public void refresh(WorkerHeartbeat workerHeartbeat) {
        address = workerHeartbeat.getWorkerAddress();
        lastActiveTime = workerHeartbeat.getHeartbeatTime();
        protocol = workerHeartbeat.getProtocol();
        client = workerHeartbeat.getClient();
        tag = workerHeartbeat.getTag();
        systemMetrics = workerHeartbeat.getSystemMetrics();
        containerInfos = workerHeartbeat.getContainerInfos();

        lightTaskTrackerNum = workerHeartbeat.getLightTaskTrackerNum();
        heavyTaskTrackerNum = workerHeartbeat.getHeavyTaskTrackerNum();

        if (workerHeartbeat.isOverload()) {
            overloading = true;
            lastOverloadTime = workerHeartbeat.getHeartbeatTime();
            log.warn("[WorkerInfo] worker {} is overload!", getAddress());
        } else {
            overloading = false;
        }
    }

    public boolean timeout() {
        long timeout = System.currentTimeMillis() - lastActiveTime;
        return timeout > WORKER_TIMEOUT_MS;
    }

    public boolean overload() {
        return overloading;
    }
}
