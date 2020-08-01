package com.github.kfcfans.powerjob.server.service.ha;

import com.github.kfcfans.powerjob.common.model.DeployedContainerInfo;
import com.github.kfcfans.powerjob.common.model.SystemMetrics;
import com.github.kfcfans.powerjob.common.request.WorkerHeartbeat;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 管理Worker集群状态
 *
 * @author tjq
 * @since 2020/4/5
 */
@Slf4j
public class ClusterStatusHolder {

    // 集群所属的应用名称
    private final String appName;
    // 集群中所有机器的健康状态
    private final Map<String, SystemMetrics> address2Metrics;
    // 集群中所有机器的容器部署状态 containerId -> (workerAddress -> containerInfo)
    private Map<Long, Map<String, DeployedContainerInfo>> containerId2Infos;
    // 集群中所有机器的最后心跳时间
    private final Map<String, Long> address2ActiveTime;

    private static final long WORKER_TIMEOUT_MS = 60000;

    public ClusterStatusHolder(String appName) {
        this.appName = appName;
        address2Metrics = Maps.newConcurrentMap();
        address2ActiveTime = Maps.newConcurrentMap();
        containerId2Infos = Maps.newConcurrentMap();
    }

    /**
     * 更新 worker 机器的状态
     */
    public void updateStatus(WorkerHeartbeat heartbeat) {

        String workerAddress = heartbeat.getWorkerAddress();
        long heartbeatTime = heartbeat.getHeartbeatTime();

        Long oldTime = address2ActiveTime.getOrDefault(workerAddress, -1L);
        if (heartbeatTime < oldTime) {
            log.warn("[ClusterStatusHolder-{}] receive the expired heartbeat from {}, serverTime: {}, heartTime: {}", appName, heartbeat.getWorkerAddress(), System.currentTimeMillis(), heartbeat.getHeartbeatTime());
            return;
        }

        address2ActiveTime.put(workerAddress, heartbeatTime);
        address2Metrics.put(workerAddress, heartbeat.getSystemMetrics());

        List<DeployedContainerInfo> containerInfos = heartbeat.getContainerInfos();
        if (!CollectionUtils.isEmpty(containerInfos)) {
            containerInfos.forEach(containerInfo -> {
                Map<String, DeployedContainerInfo> infos = containerId2Infos.computeIfAbsent(containerInfo.getContainerId(), ignore -> Maps.newConcurrentMap());
                infos.put(workerAddress, containerInfo);
            });
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
                log.info("[ClusterStatusHolder] worker(address={},metrics={}) was filtered because of timeout, last active time is {}.", address, metrics, address2ActiveTime.get(address));
                return;
            }
            // 判断指标
            if (metrics.available(minCPUCores, minMemorySpace, minDiskSpace)) {
                workers.add(address);
            }else {
                log.info("[ClusterStatusHolder] worker(address={},metrics={}) was filtered by config(minCPUCores={},minMemory={},minDiskSpace={})", address, metrics, minCPUCores, minMemorySpace, minDiskSpace);
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

    /**
     * 获取当前该Worker集群容器的部署情况
     * @param containerId 容器ID
     * @return 该容器的部署情况
     */
    public List<DeployedContainerInfo> getDeployedContainerInfos(Long containerId) {
        List<DeployedContainerInfo> res = Lists.newLinkedList();
        containerId2Infos.getOrDefault(containerId, Collections.emptyMap()).forEach((address, info) -> {
            info.setWorkerAddress(address);
            res.add(info);
        });
        return res;
    }

    /**
     * 释放所有本地存储的容器信息（该操作会导致短暂的 listDeployedContainer 服务不可用）
     */
    public void release() {
        log.info("[ClusterStatusHolder-{}] clean the containerInfos, listDeployedContainer service may down about 1min~", appName);
        // 丢弃原来的所有数据，准备重建
        containerId2Infos = Maps.newConcurrentMap();

        // 丢弃超时机器的信息
        List<String> timeoutAddress = Lists.newLinkedList();
        address2Metrics.forEach((addr, lastActiveTime) -> {
            if (timeout(addr)) {
                timeoutAddress.add(addr);
            }
        });
        if (!timeoutAddress.isEmpty()) {
            log.info("[ClusterStatusHolder-{}] detective timeout workers({}), try to release their infos.", appName, timeoutAddress);
            timeoutAddress.forEach(addr -> {
                address2ActiveTime.remove(addr);
                address2Metrics.remove(addr);
            });
        }
    }

    private boolean timeout(String address) {
        // 排除超时机器
        Long lastActiveTime = address2ActiveTime.getOrDefault(address, -1L);
        long timeout = System.currentTimeMillis() - lastActiveTime;
        return timeout > WORKER_TIMEOUT_MS;
    }
}
