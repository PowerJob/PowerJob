package tech.powerjob.server.auth.interceptor;

import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import tech.powerjob.common.Loggers;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.server.auth.PowerJobUser;
import tech.powerjob.server.auth.anno.ApiPermission;
import tech.powerjob.server.auth.service.PowerJobAuthService;

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
@Component
public class PowerJobAuthInterceptor implements HandlerInterceptor {
    @Resource
    private PowerJobAuthService powerJobAuthService;

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
        final Optional<PowerJobUser> loginUserOpt = powerJobAuthService.parse(request);

        // 未登录前先使用302重定向到登录页面
        if (!loginUserOpt.isPresent()) {
            response.setStatus(302);
            response.setHeader("location", request.getContextPath() + "/login");
            return false;
        }

        // 登陆用户进行权限校验
        final PowerJobUser powerJobUser = loginUserOpt.get();
        final boolean hasPermission = powerJobAuthService.hasPermission(request, powerJobUser, apiPermissionAnno);
        if (hasPermission) {
            return true;
        }

        final String resourceName = parseResourceName(apiPermissionAnno, handlerMethod);
        Loggers.WEB.info("[PowerJobAuthInterceptor] user[{}] has no permission to access: {}", powerJobUser.getUsername(), resourceName);

        throw new PowerJobException("Permission denied!");
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
}
