package tech.powerjob.server.auth.login;

import tech.powerjob.server.auth.PowerJobUser;

import java.util.Optional;

/**
 * PowerJobLoginService
 *
 * @author tjq
 * @since 2023/3/21
 */
public interface PowerJobLoginService {

    /**
     * 执行真正的登陆操作
     * @param loginContext 登录上下文
     * @return PowerJob 用户
     */
    Optional<PowerJobUser> login(LoginContext loginContext);

    /**
     * 从 JWT 信息中解析用户
     * @param loginContext 登录上下文
     * @return PowerJob 用户
     */
    Optional<PowerJobUser> parse(LoginContext loginContext);
}
