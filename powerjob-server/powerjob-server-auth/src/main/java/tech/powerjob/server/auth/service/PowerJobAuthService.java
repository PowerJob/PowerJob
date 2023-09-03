package tech.powerjob.server.auth.service;

import tech.powerjob.server.auth.LoginContext;
import tech.powerjob.server.auth.PowerJobUser;
import tech.powerjob.server.auth.interceptor.ApiPermission;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

/**
 * PowerJobLoginService
 *
 * @author tjq
 * @since 2023/3/21
 */
public interface PowerJobAuthService {

    List<String> supportTypes();

    /**
     * 开始登陆
     * @param loginContext 请求
     * @return 转发 or 重定向到真正的登陆页
     */
    String startLogin(LoginContext loginContext);

    /**
     * 执行真正的登陆操作
     * @param loginContext 登录上下文
     * @return PowerJob 用户
     */
    PowerJobUser tryLogin(LoginContext loginContext);
    

    /**
     * 从 JWT 信息中解析用户
     * @param httpServletRequest httpServletRequest
     * @return PowerJob 用户
     */
    Optional<PowerJobUser> ifLogin(HttpServletRequest httpServletRequest);

    /**
     * 判断用户是否有访问权限
     * @param request 上下文请求
     * @param handler hander
     * @param user 用户
     * @param apiPermission 权限描述
     * @return true or false
     */
    boolean hasPermission(HttpServletRequest request, Object handler, PowerJobUser user, ApiPermission apiPermission);
}
