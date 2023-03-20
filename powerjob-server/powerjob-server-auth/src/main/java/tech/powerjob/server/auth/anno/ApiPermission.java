package tech.powerjob.server.auth.anno;

import tech.powerjob.server.auth.Permission;

/**
 * API 权限
 *
 * @author tjq
 * @since 2023/3/20
 */
public @interface ApiPermission {
    /**
     * API 名称
     * @return 空使用服务.方法名代替
     */
    String name() default "";

    /**
     * 需要的权限
     * @return 权限
     */
    Permission requiredPermission();
}
