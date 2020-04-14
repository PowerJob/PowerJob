package com.github.kfcfans.oms.server.service.ha;

import com.github.kfcfans.common.model.SystemMetrics;
import com.github.kfcfans.common.request.WorkerHeartbeat;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 管理Worker集群状态
 *
 * @author tjq
 * @since 2020/4/5
 */
@Slf4j
public class ClusterStatusHolder {

    // 集群所属的应用名称
    private String appName;
    // 集群中所有机器的健康状态
    private Map<String, SystemMetrics> address2Metrics;
    // 集群中所有机器的最后心跳时间
    private Map<String, Long> address2ActiveTime;

    private static final long WORKER_TIMEOUT_MS = 30000;

    public ClusterStatusHolder(String appName) {
        this.appName = appName;
        address2Metrics = Maps.newConcurrentMap();
        address2ActiveTime = Maps.newConcurrentMap();
    }

    /**
     * 更新 worker 机器的状态
     */
    public void updateStatus(WorkerHeartbeat heartbeat) {

        String workerAddress = heartbeat.getWorkerAddress();
        long heartbeatTime = heartbeat.getHeartbeatTime();

        address2Metrics.put(workerAddress, heartbeat.getSystemMetrics());
        Long oldTime = address2ActiveTime.getOrDefault(workerAddress, -1L);
        if (heartbeatTime > oldTime) {
            address2ActiveTime.put(workerAddress, heartbeatTime);
        }
    }

    /**
     * 获取当前所有可用的 Worker
     * @param minCPUCores 最低CPU核心数量
     * @param minMemorySpace 最低内存可用空间，单位GB
     * @param minDiskSpace 最低磁盘可用空间，单位GB
     * @return List<Worker>
     */
    public List<String> getSortedAvailableWorker(double minCPUCores, double minMemorySpace, double minDiskSpace) {
        List<String> workers = Lists.newLinkedList();

        address2Metrics.forEach((address, metrics) -> {

            if (timeout(address)) {
                return;
            }
            // 判断指标
            if (metrics.available(minCPUCores, minMemorySpace, minDiskSpace)) {
                workers.add(address);
            }
        });

        // 按机器健康度排序
        workers.sort((o1, o2) -> address2Metrics.get(o2).calculateScore() - address2Metrics.get(o1).calculateScore());

        return workers;
    }

    /**
     * 获取整个集群的简介
     * @return 获取集群简介
     */
    public String getClusterDescription() {
        return String.format("appName:%s,clusterStatus:%s", appName, address2Metrics.toString());
    }

    /**
     * 获取当前连接的的机器详情
     * @return map
     */
    public Map<String, SystemMetrics> getActiveWorkerInfo() {
        Map<String, SystemMetrics> res = Maps.newHashMap();
        address2Metrics.forEach((address, metrics) -> {
            if (!timeout(address)) {
                res.put(address, metrics);
            }
        });
        return res;
    }

    private boolean timeout(String address) {
        // 排除超时机器
        Long lastActiveTime = address2ActiveTime.getOrDefault(address, -1L);
        long timeout = System.currentTimeMillis() - lastActiveTime;
        return timeout > WORKER_TIMEOUT_MS;
    }
}
