package tech.powerjob.server.auth.service;

import tech.powerjob.server.auth.LoginContext;
import tech.powerjob.server.auth.PowerJobUser;
import tech.powerjob.server.auth.anno.ApiPermission;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * PowerJobLoginService
 *
 * @author tjq
 * @since 2023/3/21
 */
public interface PowerJobAuthService {

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
    Optional<PowerJobUser> parse(HttpServletRequest httpServletRequest);

    boolean hasPermission(PowerJobUser user, ApiPermission apiPermission);
}
