package tech.powerjob.server.persistence.remote.model;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

/**
 * 用户角色表
 *
 * @author tjq
 * @since 2023/3/20
 */
@Data
@Entity
@Table(indexes = {
        @Index(name = "uidx01_user_id", columnList = "userId")
})
public class UserRoleDO {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    @Column(columnDefinition = "bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID'")
    private Long id;

    @Column(columnDefinition = "bigint DEFAULT NULL COMMENT '授予角色的用户ID'")
    private Long userId;

    @Column(columnDefinition = "int DEFAULT NULL COMMENT '权限范围，namespace 还是 app'")
    private Integer scope;

    @Column(columnDefinition = "bigint DEFAULT NULL COMMENT '和 scope 一起组成授权目标，比如某个 app 或 某个 namespace'")
    private Long target;

    @Column(columnDefinition = "int DEFAULT NULL COMMENT '角色，比如 Observer'")
    private Integer role;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '扩展字段'")
    private String extra;

    @Column(columnDefinition = "datetime(6) DEFAULT NULL COMMENT '创建时间'")
    private Date gmtCreate;

    @Column(columnDefinition = "datetime(6) DEFAULT NULL COMMENT '更新时间'")
    private Date gmtModified;
}