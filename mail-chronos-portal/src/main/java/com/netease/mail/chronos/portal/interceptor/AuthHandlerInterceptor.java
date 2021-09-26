package com.netease.mail.chronos.portal.interceptor;

import com.netease.mail.chronos.base.constant.AuthConstant;
import com.netease.mail.chronos.portal.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Echo009
 * @since 2021/9/6
 */
@SuppressWarnings("ALL")
@Component
@Slf4j
@RequiredArgsConstructor
public class AuthHandlerInterceptor implements HandlerInterceptor {


    private final AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authStr = request.getHeader(AuthConstant.AUTH_HEADER_NAME);
        if (StringUtils.isBlank(authStr)){
            log.error("[opt:permission check,message:reject,no auth info]");
            response.setStatus(HttpStatus.SC_UNAUTHORIZED);
            return false;
        }
        if (!authService.checkPermission(authStr)){
            log.error("[opt:permission check,message:reject,invalid auth info]");
            response.setStatus(HttpStatus.SC_UNAUTHORIZED);
            return false;
        }
        return true;
    }
}




