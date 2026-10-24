package tech.powerjob.server.persistence.remote.model;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

/**
 * 用户信息表
 * PowerJob 自身维护的全部用户体系数据
 * 5.0.0 可能不兼容改动：为了支持第三方登录，需要通过 username 与第三方登录系统做匹配，该列需要声明为唯一索引，确保全局唯一
 *
 * @author tjq
 * @since 2020/4/12
 */
@Data
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uidx01_user_name", columnNames = {"username"})
},
        indexes = {
                @Index(name = "uidx02_user_info", columnList = "email")
        })
public class UserInfoDO {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    @Column(columnDefinition = "bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID'")
    private Long id;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '账号类型'")
    private String accountType;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '用户名'")
    private String username;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '昵称'")
    private String nick;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '密码'")
    private String password;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '手机号'")
    private String phone;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '邮箱地址'")
    private String email;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT 'webHook'")
    private String webHook;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT 'JWT 登录的二次校验信息'")
    private String tokenLoginVerifyInfo;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '扩展字段 for 第三方'")
    private String extra;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '原始账号 username'")
    private String originUsername;

    @Column(columnDefinition = "int DEFAULT NULL COMMENT '账号当前状态'")
    private Integer status;

    @Column(columnDefinition = "datetime(6) DEFAULT NULL COMMENT '创建时间'")
    private Date gmtCreate;

    @Column(columnDefinition = "datetime(6) DEFAULT NULL COMMENT '更新时间'")
    private Date gmtModified;
}