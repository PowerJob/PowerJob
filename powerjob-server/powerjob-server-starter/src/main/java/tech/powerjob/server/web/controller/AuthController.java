package tech.powerjob.server.web.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.server.auth.login.LoginTypeInfo;

import java.util.List;

/**
 * 登录 & 权限相关
 *
 * @author tjq
 * @since 2023/4/16
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @GetMapping("/listSupportLoginTypes")
    public ResultDTO<List<LoginTypeInfo>> listSupportLoginTypes() {
        return null;
    }

}
