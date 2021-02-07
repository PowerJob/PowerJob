package com.github.kfcfans.powerjob.server.service.ha;

import com.alibaba.fastjson.JSON;
import com.github.kfcfans.powerjob.common.model.DeployedContainerInfo;
import com.github.kfcfans.powerjob.common.model.SystemMetrics;
import com.github.kfcfans.powerjob.common.model.WorkerInfo;
import com.github.kfcfans.powerjob.common.request.WorkerHeartbeat;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    // 集群中所有机器的信息
    private final Map<String, WorkerInfo> address2WorkerInfo;
    // 集群中所有机器的容器部署状态 containerId -> (workerAddress -> containerInfo)
    private Map<Long, Map<String, DeployedContainerInfo>> containerId2Infos;

    private static final long WORKER_TIMEOUT_MS = 60000;

    public ClusterStatusHolder(String appName) {
        this.appName = appName;
        address2WorkerInfo = Maps.newConcurrentMap();
        containerId2Infos = Maps.newConcurrentMap();
    }

    /**
     * 更新 worker 机器的状态
     */
    public void updateStatus(WorkerHeartbeat heartbeat) {

        String workerAddress = heartbeat.getWorkerAddress();
        long heartbeatTime = heartbeat.getHeartbeatTime();

        WorkerInfo workerInfo = address2WorkerInfo.computeIfAbsent(workerAddress, ignore -> {
            WorkerInfo wf = new WorkerInfo();
            wf.refresh(heartbeat);
            return wf;
        });
        long oldTime = workerInfo.getLastActiveTime();
        if (heartbeatTime < oldTime) {
            log.warn("[ClusterStatusHolder-{}] receive the expired heartbeat from {}, serverTime: {}, heartTime: {}", appName, heartbeat.getWorkerAddress(), System.currentTimeMillis(), heartbeat.getHeartbeatTime());
            return;
        }

        workerInfo.refresh(heartbeat);

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
    public List<WorkerInfo> getSortedAvailableWorkers(double minCPUCores, double minMemorySpace, double minDiskSpace) {
        List<WorkerInfo> workers = getAvailableWorkers(minCPUCores, minMemorySpace, minDiskSpace);

        // 按机器健康度排序
        workers.sort((o1, o2) -> o2 .getSystemMetrics().calculateScore() - o1.getSystemMetrics().calculateScore());

        return workers;
    }

    public List<WorkerInfo> getAvailableWorkers(double minCPUCores, double minMemorySpace, double minDiskSpace) {
        List<WorkerInfo> workerInfos = Lists.newArrayList();
        address2WorkerInfo.forEach((address, workerInfo) -> {

            if (timeout(address)) {
                log.info("[ClusterStatusHolder] worker(address={},metrics={}) was filtered because of timeout, last active time is {}.", address, workerInfo.getSystemMetrics(), workerInfo.getLastActiveTime());
                return;
            }
            // 判断指标
            SystemMetrics metrics = workerInfo.getSystemMetrics();
            if (metrics.available(minCPUCores, minMemorySpace, minDiskSpace)) {
                workerInfos.add(workerInfo);
            }else {
                log.info("[ClusterStatusHolder] worker(address={},metrics={}) was filtered by config(minCPUCores={},minMemory={},minDiskSpace={})", address, metrics, minCPUCores, minMemorySpace, minDiskSpace);
            }
        });
        return workerInfos;
    }

    /**
     * 获取整个集群的简介
     * @return 获取集群简介
     */
    public String getClusterDescription() {
        return String.format("appName:%s,clusterStatus:%s", appName, JSON.toJSONString(address2WorkerInfo));
    }

    /**
     * 获取当前连接的的机器详情
     * @return map
     */
    public Map<String, WorkerInfo> getActiveWorkerInfo() {
        Map<String, WorkerInfo> res = Maps.newHashMap();
        address2WorkerInfo.forEach((address, workerInfo) -> {
            if (!timeout(address)) {
                res.put(address, workerInfo);
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
        address2WorkerInfo.forEach((addr, workerInfo) -> {
            if (timeout(addr)) {
                timeoutAddress.add(addr);
            }
        });

        if (!timeoutAddress.isEmpty()) {
            log.info("[ClusterStatusHolder-{}] detective timeout workers({}), try to release their infos.", appName, timeoutAddress);
            timeoutAddress.forEach(address2WorkerInfo::remove);
        }
    }

    private boolean timeout(String address) {
        // 排除超时机器
        return Optional.ofNullable(address2WorkerInfo.get(address))
                .map(workerInfo -> {
                    long timeout = System.currentTimeMillis() - workerInfo.getLastActiveTime();
                    return timeout > WORKER_TIMEOUT_MS;
                })
                .orElse(true);

    }
}
