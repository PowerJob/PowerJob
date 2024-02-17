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
    private Long id;

    /**
     * 空间唯一标识
     */
    private String code;

    /**
     * 空间名称，比如中文描述（XX部门XX空间）
     */
    private String name;

    /**
     * 鉴权 token，后续 OpenAPI 调用需要
     */
    private String token;

    private Integer status;

    /**
     * 部门，组织架构相关属性。
     * 预留数据库字段方便基于组织架构二次开发
     */
    private String dept;

    /**
     * 标签，扩展性之王，多值逗号分割
     */
    private String tags;

    /**
     * 扩展字段
     */
    private String extra;

    private Date gmtCreate;

    private Date gmtModified;

    private Long creator;

    private Long modifier;
}
