package tech.powerjob.server.persistence.remote.model;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

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
@Table(indexes = {@Index(name = "idx01_container_info", columnList = "appId")})
public class ContainerInfoDO {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    @Column(columnDefinition = "bigint NOT NULL AUTO_INCREMENT COMMENT '容器ID'")
    private Long id;

    @Column(columnDefinition = "bigint DEFAULT NULL COMMENT '所属的应用ID'")
    private Long appId;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '容器名称'")
    private String containerName;

    @Column(columnDefinition = "int DEFAULT NULL COMMENT '容器类型，枚举值为 ContainerSourceType'")
    private Integer sourceType;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '由 sourceType 决定，JarFile -> String，存储文件名称；Git -> JSON，包括 URL，branch，username，password'")
    private String sourceInfo;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '版本 （Jar包使用md5，Git使用commitId，前者32位，后者40位，不会产生碰撞）'")
    private String version;

    @Column(columnDefinition = "int DEFAULT NULL COMMENT '状态，枚举值为 ContainerStatus'")
    private Integer status;

    @Column(columnDefinition = "datetime(6) DEFAULT NULL COMMENT '上一次部署时间'")
    private Date lastDeployTime;

    @Column(columnDefinition = "datetime(6) DEFAULT NULL COMMENT '创建时间'")
    private Date gmtCreate;

    @Column(columnDefinition = "datetime(6) DEFAULT NULL COMMENT '更新时间'")
    private Date gmtModified;
}