package tech.powerjob.server.auth.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import tech.powerjob.common.exception.ImpossibleException;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.server.auth.LoginUserHolder;
import tech.powerjob.server.auth.Permission;
import tech.powerjob.server.auth.PowerJobUser;
import tech.powerjob.server.auth.RoleScope;
import tech.powerjob.common.enums.ErrorCodes;
import tech.powerjob.server.auth.common.PowerJobAuthException;
import tech.powerjob.server.auth.common.utils.HttpServletUtils;
import tech.powerjob.server.auth.service.login.PowerJobLoginService;
import tech.powerjob.server.auth.service.permission.PowerJobPermissionService;
import tech.powerjob.server.common.Loggers;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * login auth and permission check
 *
 * @author tjq
 * @since 2023/3/25
 */
@Slf4j
@Component
public class PowerJobAuthInterceptor implements HandlerInterceptor {
    @Resource
    private PowerJobLoginService powerJobLoginService;
    @Resource
    private PowerJobPermissionService powerJobPermissionService;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,@NonNull HttpServletResponse response,@NonNull Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        final Method method = handlerMethod.getMethod();
        final ApiPermission apiPermissionAnno = method.getAnnotation(ApiPermission.class);

        // 无注解代表不需要权限，无需登陆直接访问
        if (apiPermissionAnno == null) {
            return true;
        }

        // 尝试直接解析登陆
        final Optional<PowerJobUser> loginUserOpt = powerJobLoginService.ifLogin(request);

        // 未登录直接报错，返回固定状态码，前端拦截后跳转到登录页
        if (!loginUserOpt.isPresent()) {
            throw new PowerJobAuthException(ErrorCodes.USER_NOT_LOGIN);
        }

        // 登陆用户进行权限校验
        final PowerJobUser powerJobUser = loginUserOpt.get();

        // 写入上下文
        LoginUserHolder.set(powerJobUser);

        Permission requiredPermission = parsePermission(request, handler, apiPermissionAnno);
        RoleScope roleScope = apiPermissionAnno.roleScope();
        Long targetId = null;

        if (RoleScope.NAMESPACE.equals(roleScope)) {

            final String namespaceIdStr = HttpServletUtils.fetchFromHeader("NamespaceId", request);
            if (StringUtils.isNotEmpty(namespaceIdStr)) {
                targetId = Long.valueOf(namespaceIdStr);
            }
        }

        if (RoleScope.APP.equals(roleScope)) {
            final String appIdStr = HttpServletUtils.fetchFromHeader("AppId", request);
            if (StringUtils.isNotEmpty(appIdStr)) {
                targetId = Long.valueOf(appIdStr);
            }
        }


        final boolean hasPermission = powerJobPermissionService.hasPermission(powerJobUser.getId(), roleScope, targetId, requiredPermission);
        if (hasPermission) {
            return true;
        }

        final String resourceName = parseResourceName(apiPermissionAnno, handlerMethod);
        Loggers.WEB.info("[PowerJobAuthInterceptor] user[{}] has no permission to access: {}", powerJobUser.getUsername(), resourceName);

        throw new PowerJobException("Permission denied!");
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, Exception ex) throws Exception {
        LoginUserHolder.clean();
    }

    private static String parseResourceName(ApiPermission apiPermission, HandlerMethod handlerMethod) {
        final String name = apiPermission.name();
        if (StringUtils.isNotEmpty(name)) {
            return name;
        }
        try {
            final String clzName = handlerMethod.getBean().getClass().getSimpleName();
            final String methodName = handlerMethod.getMethod().getName();
            return String.format("%s_%s", clzName, methodName);
        } catch (Exception ignore) {
        }
        return "UNKNOWN";
    }

    private static Permission parsePermission(HttpServletRequest request, Object handler, ApiPermission apiPermission) {
        Class<? extends DynamicPermissionPlugin> dynamicPermissionPlugin = apiPermission.dynamicPermissionPlugin();
        if (EmptyPlugin.class.equals(dynamicPermissionPlugin)) {
            return apiPermission.requiredPermission();
        }
        try {
            DynamicPermissionPlugin dynamicPermission = dynamicPermissionPlugin.getDeclaredConstructor().newInstance();
            return dynamicPermission.calculate(request, handler);
        } catch (Throwable t) {
            log.error("[PowerJobAuthService] process dynamicPermissionPlugin failed!", t);
            ExceptionUtils.rethrow(t);
        }
        throw new ImpossibleException();
    }
}
