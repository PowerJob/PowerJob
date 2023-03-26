package tech.powerjob.server.auth;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * PowerJob 的 登陆用户
 *
 * @author tjq
 * @since 2023/3/20
 */
@Getter
@Setter
@ToString
public class PowerJobUser implements Serializable {

    private Long id;

    private String username;

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
     * 扩展字段
     */
    private String extra;

    /* ************** 以上为数据库字段 ************** */

    private String jwtToken;
}
