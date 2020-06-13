package com.github.kfcfans.powerjob.common.request;

import com.github.kfcfans.powerjob.common.OmsSerializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Worker部署Container请求
 *
 * @author tjq
 * @since 2020/5/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerDeployContainerRequest implements OmsSerializable {

    /**
     * 容器ID
     */
    private Long containerId;
    /**
     * 容器名称
     */
    private String containerName;
    /**
     * 文件名（MD5值），用于做版本校验和文件下载
     */
    private String version;
    /**
     * 下载地址
     */
    private String downloadURL;
}
