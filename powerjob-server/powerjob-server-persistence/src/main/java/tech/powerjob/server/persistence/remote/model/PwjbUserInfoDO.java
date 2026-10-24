package tech.powerjob.server.persistence.remote.model;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

/**
 * PowerJob 自建登录体系的用户表，只存储使用 PowerJob 自带登录方式登录的用户信息
 *
 * @author tjq
 * @since 2024/2/13
 */
@Data
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uidx01_username", columnNames = {"username"})
})
public class PwjbUserInfoDO {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    @Column(columnDefinition = "bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID'")
    private Long id;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '用户名'")
    private String username;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '密码'")
    private String password;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '扩展字段'")
    private String extra;

    @Column(columnDefinition = "datetime(6) DEFAULT NULL COMMENT '创建时间'")
    private Date gmtCreate;

    @Column(columnDefinition = "datetime(6) DEFAULT NULL COMMENT '更新时间'")
    private Date gmtModified;
}