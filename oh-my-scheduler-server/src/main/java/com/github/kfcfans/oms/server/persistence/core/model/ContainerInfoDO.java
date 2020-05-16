package com.github.kfcfans.oms.server.persistence.core.model;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

/**
 * 容器（jar容器）信息表
 *
 * @author tjq
 * @since 2020/5/15
 */
@Data
@Entity
@Table(name = "container_info", uniqueConstraints = {@UniqueConstraint(name = "containerNameUK", columnNames = {"containerName"})})
public class ContainerInfoDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 所属的应用ID
    private Long appId;

    private String containerName;

    // 容器类型，枚举值为 ContainerSourceType
    private Integer sourceType;
    // 由 sourceType 决定，JarFile -> String，存储文件名称；Git -> JSON，包括 URL，branch，username，password
    private String sourceInfo;

    // jar的MD5，唯一，作为 GridFS 的文件名
    private String md5;

    // 状态，枚举值为 ContainerStatus
    private Integer status;

    private Date gmtCreate;
    private Date gmtModified;
}
