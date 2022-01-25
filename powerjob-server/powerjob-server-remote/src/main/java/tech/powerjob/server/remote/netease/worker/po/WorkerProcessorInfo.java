package tech.powerjob.server.remote.netease.worker.po;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.powerjob.common.annotation.NetEaseCustomFeature;
import tech.powerjob.common.enums.CustomFeatureEnum;
import tech.powerjob.common.request.WorkerProcessorInfoReportReq;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Echo009
 * @since 2022/1/25
 */
@Slf4j
@RequiredArgsConstructor
@NetEaseCustomFeature(CustomFeatureEnum.PROCESSOR_AUTO_REGISTRY)
public class WorkerProcessorInfo {

    private final String address;

    private Set<String> processors;


    public void refresh(WorkerProcessorInfoReportReq req){
        this.processors = new LinkedHashSet<>();
        this.processors.addAll(req.getProcessors());
        log.info("[WorkerProcessorInfo] Init WorkerProcessorInfo of worker({})",this.address);
    }

    public boolean contains(String processor){
        return this.processors.contains(processor);
    }
}
