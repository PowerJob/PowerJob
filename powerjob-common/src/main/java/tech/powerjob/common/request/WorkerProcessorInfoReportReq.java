package tech.powerjob.common.request;

import lombok.Data;
import tech.powerjob.common.PowerSerializable;
import tech.powerjob.common.annotation.NetEaseCustomFeature;
import tech.powerjob.common.enums.CustomFeatureEnum;

import java.util.Set;

/**
 * @author Echo009
 * @since 2022/1/25
 */
@NetEaseCustomFeature(CustomFeatureEnum.PROCESSOR_AUTO_REGISTRY)
@Data
public class WorkerProcessorInfoReportReq implements PowerSerializable {

    private Long appId;

    private String appName;

    private String workerAddress;

    private Set<String> processors;

    private long reportTime;

}
