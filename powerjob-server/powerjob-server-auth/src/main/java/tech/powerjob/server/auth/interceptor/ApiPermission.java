package tech.powerjob.server.auth.interceptor;

import tech.powerjob.server.auth.Permission;
import tech.powerjob.server.auth.RoleScope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API 权限
 *
 * @author tjq
 * @since 2023/3/20
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiPermission {
    /**
     * API 名称
     * @return 空使用服务.方法名代替
     */
    String name() default "";

    RoleScope roleScope() default RoleScope.APP;

    /**
     * 需要的权限
     * @return 权限
     */
    Permission requiredPermission() default Permission.SU;

    /**
     * 固定权限不支持的场景，需要使用动态权限
     * @return 动态权限
     */
    Class<? extends DynamicPermissionPlugin> dynamicPermissionPlugin() default EmptyPlugin.class;

    /**
     * 新增场景，需要授权插件执行授权
     * @return 授权插件
     */
    Class<? extends GrantPermissionPlugin> grandPermissionPlugin() default EmptyPlugin.class;
}
