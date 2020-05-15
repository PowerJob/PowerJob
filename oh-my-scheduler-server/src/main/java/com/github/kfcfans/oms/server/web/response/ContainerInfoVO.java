package com.github.kfcfans.oms.server.web.response;

import lombok.Data;

import java.util.Date;

/**
 * 容器信息 视图层展示对象
 *
 * @author tjq
 * @since 2020/5/15
 */
@Data
public class ContainerInfoVO {
    private Long id;

    // 所属的应用ID
    private Long appId;

    private String containerName;

    // 容器类型，枚举值为 ContainerSourceType
    private Integer sourceType;
    // 由 sourceType 决定，JarFile -> String，存储文件名称；Git -> JSON，包括 URL，branch，username，password
    private String sourceInfo;

    // 文件名称（jar的MD5，唯一，作为 GridFS 的文件名）
    private String fileName;

    // 状态，枚举值为 ContainerStatus
    private Integer status;

    private Date gmtCreate;
    private Date gmtModified;
}
