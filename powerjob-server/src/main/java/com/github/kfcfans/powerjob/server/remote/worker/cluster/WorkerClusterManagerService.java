package com.github.kfcfans.powerjob.server.remote.worker.cluster;

import com.github.kfcfans.powerjob.common.model.DeployedContainerInfo;
import com.github.kfcfans.powerjob.common.request.WorkerHeartbeat;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 管理 worker 集群信息
 *
 * @author tjq
 * @since 2020/4/5
 */
@Slf4j
public class WorkerClusterManagerService {

    // 存储Worker健康信息，appId -> ClusterStatusHolder
    private static final Map<Long, ClusterStatusHolder> appId2ClusterStatus = Maps.newConcurrentMap();

    /**
     * 更新状态
     * @param heartbeat Worker的心跳包
     */
    public static void updateStatus(WorkerHeartbeat heartbeat) {
        Long appId = heartbeat.getAppId();
        String appName = heartbeat.getAppName();
        ClusterStatusHolder clusterStatusHolder = appId2ClusterStatus.computeIfAbsent(appId, ignore -> new ClusterStatusHolder(appName));
        clusterStatusHolder.updateStatus(heartbeat);
    }

    /**
     * 获取某个 app 下所有连接过的机器信息
     * @param appId appId
     * @return 所有连接过的机器信息列表
     */
    public static List<WorkerInfo> getWorkerInfosByAppId(Long appId) {
        ClusterStatusHolder clusterStatusHolder = appId2ClusterStatus.get(appId);
        if (clusterStatusHolder == null) {
            log.warn("[WorkerManagerService] can't find any worker for app(appId={}) yet.", appId);
            return Collections.emptyList();
        }
        return clusterStatusHolder.getAllWorkers();
    }


    public static Optional<WorkerInfo> getWorkerInfo(Long appId, String address) {
        ClusterStatusHolder clusterStatusHolder = appId2ClusterStatus.get(appId);
        if (clusterStatusHolder == null) {
            log.warn("[WorkerManagerService] can't find any worker for app(appId={}) yet.", appId);
            return Optional.empty();
        }
        return Optional.ofNullable(clusterStatusHolder.getWorkerInfo(address));
    }

    /**
     * 清理不需要的worker信息
     * @param usingAppIds 需要维护的appId，其余的数据将被删除
     */
    public static void clean(List<Long> usingAppIds) {
        Set<Long> keys = Sets.newHashSet(usingAppIds);
        appId2ClusterStatus.entrySet().removeIf(entry -> !keys.contains(entry.getKey()));
    }

    /**
     * 获取当前连接到该Server的Worker信息
     * @param appId 应用ID
     * @return Worker信息
     */
    public static Map<String, WorkerInfo> getActiveWorkerInfo(Long appId) {
        ClusterStatusHolder clusterStatusHolder = appId2ClusterStatus.get(appId);
        if (clusterStatusHolder == null) {
            return Collections.emptyMap();
        }
        return clusterStatusHolder.getActiveWorkerInfo();
    }
 
    /**
     * 获取某个应用容器的部署情况
     * @param appId 应用ID
     * @param containerId 容器ID
     * @return 部署情况
     */
    public static List<DeployedContainerInfo> getDeployedContainerInfos(Long appId, Long containerId) {
        ClusterStatusHolder clusterStatusHolder = appId2ClusterStatus.get(appId);
        if (clusterStatusHolder == null) {
            return Collections.emptyList();
        }
        return clusterStatusHolder.getDeployedContainerInfos(containerId);
    }

    /**
     * 清理缓存信息，防止 OOM
     */
    public static void cleanUp() {
        appId2ClusterStatus.values().forEach(ClusterStatusHolder::release);
    }

    public static Map<Long, ClusterStatusHolder> getAppId2ClusterStatus() {
        return appId2ClusterStatus;
    }
}
