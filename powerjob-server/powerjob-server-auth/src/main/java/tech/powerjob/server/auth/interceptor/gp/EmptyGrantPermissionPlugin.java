package tech.powerjob.server.auth.interceptor.gp;

import java.lang.reflect.Method;

/**
 * do nothing
 *
 * @author tjq
 * @since 2024/2/11
 */
public class EmptyGrantPermissionPlugin implements GrantPermissionPlugin {
    @Override
    public void grant(Object[] args, Object result, Method method, Object originBean) {

    }
}
