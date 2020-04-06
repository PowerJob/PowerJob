package com.github.kfcfans.oms.server.service.ha;

import com.github.kfcfans.common.request.WorkerHeartbeat;
import com.google.common.collect.Lists;
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
     * 选择状态最好的Worker执行任务
     * @param appId 应用ID
     * @return Worker的地址（null代表没有可用的Worker）
     */
    public static String chooseBestWorker(Long appId) {
        ClusterStatusHolder clusterStatusHolder = appId2ClusterStatus.get(appId);
        if (clusterStatusHolder == null) {
            log.warn("[WorkerManagerService] can't find any worker for {} yet.", appId);
            return null;
        }
        return clusterStatusHolder.chooseBestWorker();
    }

    /**
     * 获取当前所有可用的Worker地址
     */
    public static List<String> getAllAvailableWorker(Long appId) {
        ClusterStatusHolder clusterStatusHolder = appId2ClusterStatus.get(appId);
        if (clusterStatusHolder == null) {
            log.warn("[WorkerManagerService] can't find any worker for {} yet.", appId);
            return Collections.emptyList();
        }
        return clusterStatusHolder.getAllAvailableWorker();
    }

    /**
     * 清理不需要的worker信息
     * @param usingAppIds 需要维护的appId，其余的数据将被删除
     */
    public static void clean(List<Long> usingAppIds) {
        Set<Long> keys = Sets.newHashSet(usingAppIds);
        appId2ClusterStatus.entrySet().removeIf(entry -> !keys.contains(entry.getKey()));
    }

}
