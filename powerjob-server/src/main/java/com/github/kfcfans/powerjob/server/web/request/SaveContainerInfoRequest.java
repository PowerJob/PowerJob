package com.github.kfcfans.powerjob.server.web.request;

import com.github.kfcfans.powerjob.server.common.constans.ContainerSourceType;
import com.github.kfcfans.powerjob.server.common.constans.ContainerStatus;
import lombok.Data;

/**
 * 保存/修改 容器 请求
 *
 * @author tjq
 * @since 2020/5/15
 */
@Data
public class SaveContainerInfoRequest {

    // 容器ID，null -> 创建；否则代表修改
    private Long id;

    // 所属的应用ID
    private Long appId;

    // 容器名称
    private String containerName;

    // 容器类型，枚举值为 ContainerSourceType（JarFile/Git）
    private ContainerSourceType sourceType;
    // 由 sourceType 决定，JarFile -> String，存储文件名称；Git -> JSON，包括 URL，branch，username，password
    private String sourceInfo;

    // 状态，枚举值为 ContainerStatus（ENABLE/DISABLE）
    private ContainerStatus status;
}
