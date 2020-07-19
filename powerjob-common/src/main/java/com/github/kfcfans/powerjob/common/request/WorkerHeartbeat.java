package com.github.kfcfans.powerjob.common.request;

import com.github.kfcfans.powerjob.common.OmsSerializable;
import com.github.kfcfans.powerjob.common.model.DeployedContainerInfo;
import com.github.kfcfans.powerjob.common.model.SystemMetrics;
import lombok.Data;

import java.util.List;


/**
 * Worker 上报健康信息（worker定时发送的heartbeat）
 *
 * @author tjq
 * @since 2020/3/25
 */
@Data
public class WorkerHeartbeat implements OmsSerializable {

    // 本机地址 -> IP:port
    private String workerAddress;
    // 当前 appName
    private String appName;
    // 当前 appId
    private Long appId;
    // 当前时间
    private long heartbeatTime;
    // 当前加载的容器（容器名称 -> 容器版本）
    private List<DeployedContainerInfo> containerInfos;
    // worker 版本信息
    private String version;
    // 扩展字段
    private String extra;

    private SystemMetrics systemMetrics;
}
