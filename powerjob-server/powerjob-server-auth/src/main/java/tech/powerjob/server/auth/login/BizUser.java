package tech.powerjob.server.auth.login;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * BizUser
 *
 * @author tjq
 * @since 2023/3/21
 */
@Getter
@Setter
@ToString
public class BizUser {

    /**
     * 用户的唯一标识，用于关联到 PowerJob 的 username
     */
    private String username;

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
     * 扩展字段
     */
    private String extra;
}
