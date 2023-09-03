package tech.powerjob.server.auth.interceptor.dp;

import tech.powerjob.server.auth.Permission;

import javax.servlet.http.HttpServletRequest;

/**
 * 动态权限
 *
 * @author tjq
 * @since 2023/9/3
 */
public interface DynamicPermission {
    Permission calculate(HttpServletRequest request, Object handler);
}
