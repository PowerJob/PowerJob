package tech.powerjob.server.auth.login;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 第三方用户
 *
 * @author tjq
 * @since 2024/2/10
 */
@Data
@Accessors(chain = true)
public class ThirdPartyUser {

    /**
     * 用户的唯一标识，用于关联到 PowerJob 的 username
     */
    private String username;
    /**
     * JWT 登录的二次校验配置
     * 可空，空则代表放弃二次校验（会出现第三方登录改了密码当 PowerJob JWT 登录依然可用的情况）
     */
    private TokenLoginVerifyInfo tokenLoginVerifyInfo;

    /* ******** 以下全部选填即可，只是方便数据同步，后续都可以去 PowerJob 控制台更改 ******** */
    /**
     * 用户昵称
     */
    private String nick;
    /**
     * 手机号
     */
    private String phone;
    /**
     * 邮箱地址
     */
    private String email;
    /**
     * web 回调地址
     */
    private String webHook;
    /**
     * 扩展字段
     */
    private String extra;
}
