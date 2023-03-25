package tech.powerjob.server.auth.anno;

import tech.powerjob.server.auth.Permission;

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

    /**
     * 需要的权限
     * @return 权限
     */
    Permission requiredPermission();
}
