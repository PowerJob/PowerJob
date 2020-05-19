package com.github.kfcfans.oms.common.model;

import com.github.kfcfans.oms.common.OmsSerializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 已部署的容器信息
 *
 * @author tjq
 * @since 2020/5/18
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeployedContainerInfo implements OmsSerializable {

    // 容器ID
    private Long containerId;
    // 版本
    private String version;
    // 部署时间
    private long deployedTime;
    // 机器地址（无需上报）
    private String workerAddress;
}
