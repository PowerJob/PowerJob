package tech.powerjob.server.netease.controller;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import tech.powerjob.server.netease.config.AuthConfig;
import tech.powerjob.server.netease.po.NeteaseUserInfo;
import tech.powerjob.server.netease.service.AuthService;
import tech.powerjob.server.persistence.remote.model.UserInfoDO;
import tech.powerjob.server.persistence.remote.repository.UserInfoRepository;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;

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
        NeteaseUserInfo neteaseUserInfo = authService.obtainUserInfo(accessToken);

        // 检查账号是否存在
        UserInfoDO user = userInfoRepository.findByUsername(neteaseUserInfo.getUserName());

        // 不存在则自动创建账号，但不会分配权限
        if (user == null){
            log.warn("[auth.callback] current user ({}) not exist! will auto create account",neteaseUserInfo);
            //
            UserInfoDO userInfo = new UserInfoDO();
            HashMap<String, Object> extra = Maps.newHashMap();
            extra.put("fullName",neteaseUserInfo.getFullName());
            userInfo.setUsername(neteaseUserInfo.getUserName());
            userInfo.setEmail(neteaseUserInfo.getEmail());
            userInfo.setExtra(JSON.toJSONString(extra));
            userInfo.setPassword("*");
            userInfo.setGmtCreate(new Date());
            userInfo.setGmtModified(new Date());
            userInfoRepository.saveAndFlush(userInfo);
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
