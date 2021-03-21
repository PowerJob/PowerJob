package tech.powerjob.server.remote.worker;

import tech.powerjob.common.request.WorkerHeartbeat;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
     * 清理不需要的worker信息
     * @param usingAppIds 需要维护的appId，其余的数据将被删除
     */
    public static void clean(List<Long> usingAppIds) {
        Set<Long> keys = Sets.newHashSet(usingAppIds);
        appId2ClusterStatus.entrySet().removeIf(entry -> !keys.contains(entry.getKey()));
    }


    /**
     * 清理缓存信息，防止 OOM
     */
    public static void cleanUp() {
        appId2ClusterStatus.values().forEach(ClusterStatusHolder::release);
    }

    protected static Map<Long, ClusterStatusHolder> getAppId2ClusterStatus() {
        return appId2ClusterStatus;
    }

}
