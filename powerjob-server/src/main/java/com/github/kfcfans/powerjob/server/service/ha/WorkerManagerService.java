package com.github.kfcfans.powerjob.server.service.ha;

import com.github.kfcfans.powerjob.common.model.DeployedContainerInfo;
import com.github.kfcfans.powerjob.common.model.SystemMetrics;
import com.github.kfcfans.powerjob.common.request.WorkerHeartbeat;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Worker 管理服务
 *
 * @author tjq
 * @since 2020/4/5
 */
@Slf4j
public class WorkerManagerService {

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
     * 获取有序的当前所有可用的Worker地址（按得分高低排序，排在前面的健康度更高）
     */
    public static List<String> getSortedAvailableWorker(Long appId, double minCPUCores, double minMemorySpace, double minDiskSpace) {
        ClusterStatusHolder clusterStatusHolder = appId2ClusterStatus.get(appId);
        if (clusterStatusHolder == null) {
            log.warn("[WorkerManagerService] can't find any worker for app(appId={}) yet.", appId);
            return Collections.emptyList();
        }
        return clusterStatusHolder.getSortedAvailableWorker(minCPUCores, minMemorySpace, minDiskSpace);
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
     * 获取某个应用下的Worker集群状态描述
     * @param appId 应用ID
     * @return 集群状态描述信息
     */
    public static String getWorkerClusterStatusDescription(Long appId) {
        ClusterStatusHolder clusterStatusHolder = appId2ClusterStatus.get(appId);
        if (clusterStatusHolder == null) {
            return "CAN'T_FIND_ANY_WORKER";
        }
        return clusterStatusHolder.getClusterDescription();
    }

    /**
     * 获取当前连接到该Server的Worker信息
     * @param appId 应用ID
     * @return Worker信息
     */
    public static Map<String, SystemMetrics> getActiveWorkerInfo(Long appId) {
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
