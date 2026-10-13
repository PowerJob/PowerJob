package tech.powerjob.server.common.module;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import tech.powerjob.common.model.DeployedContainerInfo;
import tech.powerjob.common.model.SystemMetrics;
import tech.powerjob.common.model.TaskGroupStatus;
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

    /**
     * 上一次worker在线时间（取 server 端时间）
     */
    private long lastActiveTime;
    /**
     * 上一次worker在线时间（取 worker 端时间）
     */
    private long lastActiveWorkerTime;

    private String protocol;

    private String client;

    private String tag;

    private int lightTaskTrackerNum;

    private int heavyTaskTrackerNum;

    private long lastOverloadTime;

    private boolean overloading;

    private SystemMetrics systemMetrics;

    private List<DeployedContainerInfo> containerInfos;

    /**
     * Per-group task tracker status reported in heartbeat.
     * Null for legacy workers without group isolation configured.
     * Marked volatile to ensure visibility across the heartbeat-processing thread
     * and dispatch threads that call overloadForGroup().
     */
    private volatile List<TaskGroupStatus> taskGroupStatuses;

    private static final long WORKER_TIMEOUT_MS = 60000;

    public void refresh(WorkerHeartbeat workerHeartbeat) {
        address = workerHeartbeat.getWorkerAddress();
        lastActiveTime = System.currentTimeMillis();
        lastActiveWorkerTime = workerHeartbeat.getHeartbeatTime();
        protocol = workerHeartbeat.getProtocol();
        client = workerHeartbeat.getClient();
        tag = workerHeartbeat.getTag();
        systemMetrics = workerHeartbeat.getSystemMetrics();
        containerInfos = workerHeartbeat.getContainerInfos();

        lightTaskTrackerNum = workerHeartbeat.getLightTaskTrackerNum();
        heavyTaskTrackerNum = workerHeartbeat.getHeavyTaskTrackerNum();
        taskGroupStatuses = workerHeartbeat.getTaskGroupStatuses();

        if (workerHeartbeat.isOverload()) {
            overloading = true;
            lastOverloadTime = System.currentTimeMillis();
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

    /**
     * Check if this worker is overloaded for a specific task group.
     * Falls back to the global overload flag for legacy workers or unknown groups.
     */
    public boolean overloadForGroup(String groupName) {
        // Snapshot the volatile reference to avoid races with refresh()
        List<TaskGroupStatus> snapshot = taskGroupStatuses;
        if (snapshot == null || snapshot.isEmpty()) {
            // Legacy worker without group isolation: use global overload flag
            return overloading;
        }
        for (TaskGroupStatus status : snapshot) {
            if (status.getGroupName() != null && status.getGroupName().equals(groupName)) {
                return status.isOverloaded();
            }
        }
        // Unknown group on this worker: fall back to global overload flag
        return overloading;
    }
}
