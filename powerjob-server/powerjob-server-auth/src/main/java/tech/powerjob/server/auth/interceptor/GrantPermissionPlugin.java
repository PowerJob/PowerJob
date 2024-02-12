package tech.powerjob.server.auth.interceptor;

import java.lang.reflect.Method;

/**
 * 授予权限插件
 *
 * @author tjq
 * @since 2024/2/11
 */
public interface GrantPermissionPlugin {

    /**
     * 授权
     * @param args 入参
     * @param result 响应
     * @param method 被调用方法
     * @param originBean 原始对象
     */
    void grant(Object[] args, Object result, Method method, Object originBean);
}
