package tech.powerjob.server.auth.interceptor.dp;

import tech.powerjob.server.auth.Permission;

import javax.servlet.http.HttpServletRequest;

/**
 * 创建不需要权限，修改需要校验权限
 *
 * @author tjq
 * @since 2023/9/3
 */
public class ModifyOrCreateDynamicPermission implements DynamicPermission {
    @Override
    public Permission calculate(HttpServletRequest request, Object handler) {
        // TODO: 动态权限判断，新建不需要权限
        return Permission.WRITE;
    }
}
