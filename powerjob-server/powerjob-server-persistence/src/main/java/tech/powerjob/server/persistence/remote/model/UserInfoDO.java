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
    private Long id;

    /**
     * 账号类型
     */
    private String accountType;

    private String username;
    /**
     * since 5.0.0
     * 昵称（第三方登陆的 username 很难识别，方便后续展示引入 nick）
     */
    private String nick;

    private String password;
    /**
     * 手机号
     */
    private String phone;
    /**
     * 邮箱地址
     */
    private String email;
    /**
     * webHook
     */
    private String webHook;

    /**
     * JWT 登录的二次校验信息
     */
    private String tokenLoginVerifyInfo;

    /**
     * 扩展字段 for 第三方
     * PowerJob 内部不允许使用该字段
     */
    private String extra;

    /**
     * 原始账号 username
     */
    private String originUsername;

    /**
     * 账号当前状态
     */
    private Integer status;

    private Date gmtCreate;

    private Date gmtModified;
}
