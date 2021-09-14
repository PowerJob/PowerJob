package tech.powerjob.server.netease.controller;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import tech.powerjob.server.netease.config.AuthConfig;
import tech.powerjob.server.netease.service.AuthService;
import tech.powerjob.server.persistence.remote.model.UserInfoDO;
import tech.powerjob.server.persistence.remote.repository.UserInfoRepository;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Echo009
 * @since 2021/9/2
 */
@Controller
@Slf4j
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private final AuthConfig authConfig;

    private final UserInfoRepository userInfoRepository;


    /**
     * 登录回调
     * 通过 code 换取 Access Token
     * 校验 ID token
     * 通过 access_token 获取用户信息
     *
     * https://login.netease.com/download/oidc_docs/flow/authentication_response.html
     */
    @RequestMapping("/callback")
    @SneakyThrows
    public void callback(@RequestParam("code") String code, HttpServletResponse httpServletResponse){


        // 1、通过 code 换取 Access Token
        String accessToken = authService.obtainAccessToken(code);
        // 这里偷个懒
        // 2、access_token 获取用户信息
        String userName = authService.obtainUserName(accessToken);

        // 检查账号是否存在
        UserInfoDO user = userInfoRepository.findByUsername(userName);

        // 不存在则提示需要管理员创建账号
        if (user == null){

            log.warn("[auth.callback] current user {} has not privilege!",userName);
            //
            httpServletResponse.setStatus(401);
            return;
        }

        // 存在则生成 JWT，并设置 cookies
        String jwt = authService.generateJsonWebToken(user);

        Cookie cookie = new Cookie(authConfig.getTokenCookieName(), jwt);
        cookie.setMaxAge(86400);
        cookie.setDomain(authConfig.getDomain());
        cookie.setPath("/");
        httpServletResponse.addCookie(cookie);

        httpServletResponse.sendRedirect(authConfig.getHomePage());


    }

    @RequestMapping("/login")
    @SneakyThrows
    public void login(HttpServletResponse httpServletResponse){
        httpServletResponse.sendRedirect(authService.getLoginUrl());
    }


    @RequestMapping("/logout")
    @SneakyThrows
    public void logout(HttpServletResponse httpServletResponse){

        Cookie cookie = new Cookie(authConfig.getTokenCookieName(), null);
        cookie.setMaxAge(0);
        cookie.setDomain(authConfig.getDomain());
        cookie.setPath("/");
        httpServletResponse.addCookie(cookie);

        httpServletResponse.sendRedirect(authConfig.getHomePage());
    }




}
