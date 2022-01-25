package tech.powerjob.server.remote.netease.worker.po;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import tech.powerjob.common.annotation.NetEaseCustomFeature;
import tech.powerjob.common.enums.CustomFeatureEnum;
import tech.powerjob.common.request.WorkerProcessorInfoReportReq;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Echo009
 * @since 2022/1/25
 */
@Slf4j
@NetEaseCustomFeature(CustomFeatureEnum.PROCESSOR_AUTO_REGISTRY)
public class WorkerProcessorInfoHolder {

    private final String appName;

    private final Map<String,WorkerProcessorInfo> address2WorkerProcessorInfoMap ;


    public WorkerProcessorInfoHolder(String appName) {
        this.appName = appName;
        this.address2WorkerProcessorInfoMap = Maps.newConcurrentMap();
        log.info("[WorkerProcessorInfoHolder] Init WorkerProcessorInfoHolder of app({})",this.appName);
    }

    public void updateStatus(WorkerProcessorInfoReportReq req){
        WorkerProcessorInfo workerProcessorInfo = address2WorkerProcessorInfoMap.computeIfAbsent(req.getWorkerAddress(), WorkerProcessorInfo::new);
        workerProcessorInfo.refresh(req);
    }

    public void release(String workerAddress){
        log.info("[WorkerProcessorInfoHolder] release processor info of worker({}) in app({})",workerAddress,this.appName);
        address2WorkerProcessorInfoMap.remove(workerAddress);
    }

    public List<String> getAvailableWorkers(String processorClassName){
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, WorkerProcessorInfo> entry : address2WorkerProcessorInfoMap.entrySet()) {
             if (entry.getValue().contains(processorClassName)){
                 result.add(entry.getKey());
             }
        }
        return result;
    }

}
