package com.github.kfcfans.oms.server.service.ha;

import com.github.kfcfans.common.request.WorkerHeartbeat;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * Worker 管理服务
 *
 * @author tjq
 * @since 2020/4/5
 */
@Slf4j
public class WorkerManagerService {

    private static final Map<Long, ClusterStatusHolder> appName2ClusterStatus = Maps.newConcurrentMap();

    /**
     * 更新状态
     * @param heartbeat Worker的心跳包
     */
    public static void updateStatus(WorkerHeartbeat heartbeat) {
        Long appId = heartbeat.getAppId();
        String appName = heartbeat.getAppName();
        ClusterStatusHolder clusterStatusHolder = appName2ClusterStatus.computeIfAbsent(appId, ignore -> new ClusterStatusHolder(appName));
        clusterStatusHolder.updateStatus(heartbeat);
    }

    /**
     * 选择状态最好的Worker执行任务
     * @param appId 应用ID
     * @return Worker的地址（null代表没有可用的Worker）
     */
    public static String chooseBestWorker(Long appId) {
        ClusterStatusHolder clusterStatusHolder = appName2ClusterStatus.get(appId);
        if (clusterStatusHolder == null) {
            log.warn("[WorkerManagerService] can't find any worker for {} yet.", appId);
            return null;
        }
        return clusterStatusHolder.chooseBestWorker();
    }

    /**
     * 获取当前该 Server 管理的所有应用ID
     * @return List<AppId>
     */
    public static List<Long> listAppIds() {
        return Lists.newArrayList(appName2ClusterStatus.keySet());
    }

}
