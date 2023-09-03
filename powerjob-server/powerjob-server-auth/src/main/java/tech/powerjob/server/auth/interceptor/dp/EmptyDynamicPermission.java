package tech.powerjob.server.auth.interceptor.dp;

import tech.powerjob.server.auth.Permission;

import javax.servlet.http.HttpServletRequest;

/**
 * NotUseDynamicPermission
 *
 * @author tjq
 * @since 2023/9/3
 */
public class EmptyDynamicPermission implements DynamicPermission {
    @Override
    public Permission calculate(HttpServletRequest request, Object handler) {
        return null;
    }
}
