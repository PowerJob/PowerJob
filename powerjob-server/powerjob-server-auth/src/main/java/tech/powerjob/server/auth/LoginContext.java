package tech.powerjob.server.auth;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.servlet.http.HttpServletRequest;

/**
 * 登录上下文
 *
 * @author tjq
 * @since 2023/3/20
 */
@Getter
@Setter
@Accessors(chain = true)
public class LoginContext {
    /**
     * 登陆类型
     */
    private String loginType;
    /**
     * 登陆信息，取决于登陆类型，比如 PowerJob 自带的账号密码为 uid:xxx;pwd:yyy
     */
    private String loginInfo;

    private transient HttpServletRequest httpServletRequest;
}
