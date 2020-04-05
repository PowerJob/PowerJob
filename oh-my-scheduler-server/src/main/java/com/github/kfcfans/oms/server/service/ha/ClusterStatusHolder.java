package com.github.kfcfans.oms.server.service.ha;

import com.github.kfcfans.common.model.SystemMetrics;
import com.github.kfcfans.common.request.WorkerHeartbeat;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;

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
     * 选取状态最好的Worker进行任务派发
     * @return Worker的地址（null代表没有可用的Worker）
     */
    public String chooseBestWorker() {

        // 直接对 HashMap 根据Value进行排序
        List<Map.Entry<String, SystemMetrics>> entryList = Lists.newArrayList(address2Metrics.entrySet());

        // 降序排序（Comparator.comparingInt默认为升序，弃用）
        entryList.sort((o1, o2) -> o2.getValue().calculateScore() - o1.getValue().calculateScore());

        for (Map.Entry<String, SystemMetrics> entry : address2Metrics.entrySet()) {
            long lastActiveTime = address2ActiveTime.getOrDefault(entry.getKey(), -1L);
            long timeout = System.currentTimeMillis() - lastActiveTime;
            if (timeout < WORKER_TIMEOUT_MS) {
                return entry.getKey();
            }
        }

        log.warn("[ClusterStatusHolder] no worker available for {}, worker status is {}.", appName, address2Metrics);
        return null;
    }
}
