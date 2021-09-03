package tech.powerjob.server.netease.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author Echo009
 * @since 2021/9/2
 */
@Controller
@Slf4j
@RequestMapping("/auth")
public class AuthController {


    /**
     * 登录回调
     * 通过 code 换取 Access Token
     * 校验 ID token
     * 通过 access_token 获取用户信息
     */
    @RequestMapping("/callback")
    public void callback(@RequestParam("code") String code){




    }






}
