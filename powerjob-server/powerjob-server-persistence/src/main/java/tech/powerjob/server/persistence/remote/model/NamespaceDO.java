package tech.powerjob.server.persistence.remote.model;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

/**
 * 命名空间，用于组织管理 App
 *
 * @author tjq
 * @since 2023/9/3
 */
@Data
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(name = "uidx01_namespace", columnNames = {"code"})})
public class NamespaceDO {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    @Column(columnDefinition = "bigint NOT NULL AUTO_INCREMENT COMMENT '命名空间ID'")
    private Long id;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '空间唯一标识'")
    private String code;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '空间名称，比如中文描述（XX部门XX空间）'")
    private String name;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '鉴权 token，后续 OpenAPI 调用需要'")
    private String token;

    @Column(columnDefinition = "int DEFAULT NULL COMMENT '状态'")
    private Integer status;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '部门，组织架构相关属性。'")
    private String dept;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '标签，扩展性之王，多值逗号分割'")
    private String tags;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '扩展字段'")
    private String extra;

    @Column(columnDefinition = "datetime(6) DEFAULT NULL COMMENT '创建时间'")
    private Date gmtCreate;

    @Column(columnDefinition = "datetime(6) DEFAULT NULL COMMENT '更新时间'")
    private Date gmtModified;

    @Column(columnDefinition = "bigint DEFAULT NULL COMMENT '创建者ID'")
    private Long creator;

    @Column(columnDefinition = "bigint DEFAULT NULL COMMENT '修改者ID'")
    private Long modifier;
}