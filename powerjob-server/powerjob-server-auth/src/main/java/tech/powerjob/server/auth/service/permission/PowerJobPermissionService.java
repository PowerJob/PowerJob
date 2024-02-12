package tech.powerjob.server.auth.service.permission;

import tech.powerjob.server.auth.PowerJobUser;
import tech.powerjob.server.auth.Role;
import tech.powerjob.server.auth.RoleScope;
import tech.powerjob.server.auth.interceptor.ApiPermission;

import javax.servlet.http.HttpServletRequest;

/**
 * PowerJob 鉴权服务
 *
 * @author tjq
 * @since 2024/2/11
 */
public interface PowerJobPermissionService {

    /**
     * 判断用户是否有访问权限
     * @param request 上下文请求
     * @param handler hander
     * @param user 用户
     * @param apiPermission 权限描述
     * @return true or false
     */
    boolean hasPermission(HttpServletRequest request, Object handler, PowerJobUser user, ApiPermission apiPermission);

    /**
     * 授予用户权限
     * @param roleScope 权限范围
     * @param target 权限目标
     * @param userId 用户ID
     * @param role 角色
     * @param extra 其他
     */
    void grantPermission(RoleScope roleScope, Long target, Long userId, Role role, String extra);
}
