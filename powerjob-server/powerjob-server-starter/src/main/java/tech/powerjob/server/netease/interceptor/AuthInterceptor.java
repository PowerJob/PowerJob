package tech.powerjob.server.netease.interceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import tech.powerjob.server.netease.config.AuthConfig;
import tech.powerjob.server.netease.context.NeteaseContext;
import tech.powerjob.server.netease.service.AuthService;
import tech.powerjob.server.persistence.remote.model.UserInfoDO;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Echo009
 * @since 2021/9/10
 */
@SuppressWarnings("NullableProblems")
@Slf4j
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    private final AuthConfig authConfig;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (StringUtils.equalsIgnoreCase(request.getMethod(), HttpMethod.OPTIONS.name())) {
            return true;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length != 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(authConfig.getTokenCookieName())) {
                    UserInfoDO userInfoDO = authService.parseUserInfo(cookie.getValue());
                    if (userInfoDO == null){
                        log.warn("[auth.interceptor]invalid cookie {}",cookie.getValue());
                        break;
                    }
                    NeteaseContext.setCurrentUser(userInfoDO);
                    return true;
                }
            }
        }
        // 未认证，前端需拦截处理
        response.setStatus(401);
        return false;
    }


    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // clear context info
        NeteaseContext.clearCurrentUser();
    }



}
