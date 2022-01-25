package tech.powerjob.server.remote.netease.worker;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import tech.powerjob.common.annotation.NetEaseCustomFeature;
import tech.powerjob.common.enums.CustomFeatureEnum;
import tech.powerjob.common.request.WorkerProcessorInfoReportReq;
import tech.powerjob.server.remote.netease.worker.po.WorkerProcessorInfoHolder;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Echo009
 * @since 2022/1/25
 */
@Slf4j
@NetEaseCustomFeature(CustomFeatureEnum.PROCESSOR_AUTO_REGISTRY)
public class WorkerProcessorInfoManagerService {


    private static final Map<Long, WorkerProcessorInfoHolder> CONTAINER = Maps.newConcurrentMap();


    public static void updateStatus(WorkerProcessorInfoReportReq req) {
        WorkerProcessorInfoHolder workerProcessorInfoHolder = CONTAINER.computeIfAbsent(req.getAppId(), ignore -> new WorkerProcessorInfoHolder(req.getAppName()));
        workerProcessorInfoHolder.updateStatus(req);
    }

    public static void clean(List<Long> usingAppIds) {
        Set<Long> keys = Sets.newHashSet(usingAppIds);
        CONTAINER.entrySet().removeIf(entry -> !keys.contains(entry.getKey()));
    }

    public static void clean(Long appId, String workerAddress) {
        log.info("[WorkerProcessorInfo] clean WorkerProcessorInfo info,appId:{},workerAddress:{}", appId, workerAddress);
        WorkerProcessorInfoHolder workerProcessorInfoHolder = CONTAINER.get(appId);
        if (workerProcessorInfoHolder != null) {
            workerProcessorInfoHolder.release(workerAddress);
        }
    }


    public static WorkerProcessorInfoHolder getProcessorInfoHolder(Long appId){
        return CONTAINER.get(appId);
    }

}
