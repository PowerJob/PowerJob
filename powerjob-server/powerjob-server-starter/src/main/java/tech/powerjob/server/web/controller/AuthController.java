package tech.powerjob.server.web.controller;

import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.*;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.server.auth.LoginContext;
import tech.powerjob.server.auth.PowerJobUser;
import tech.powerjob.server.auth.service.PowerJobAuthService;
import tech.powerjob.server.web.request.UserLoginRequest;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

    @Resource
    private PowerJobAuthService powerJobAuthService;

    @GetMapping("/supportTypes")
    public ResultDTO<List<String>> options() {
        return ResultDTO.success(powerJobAuthService.supportTypes());
    }

    @SneakyThrows
    @GetMapping("/startLogin")
    public String tryLogin(UserLoginRequest loginRequest, HttpServletRequest request, HttpServletResponse response) {

        LoginContext loginContext = new LoginContext()
                .setLoginType(loginRequest.getType())
                .setLoginInfo(loginRequest.getLoginInfo())
                .setHttpServletRequest(request);

        final String realLoginUrl = powerJobAuthService.startLogin(loginContext);
        // 统一重定向
        response.sendRedirect(realLoginUrl);
        return null;
    }

    @PostMapping("/selfLogin")
    public ResultDTO<PowerJobUser> selfLogin(LoginContext loginContext, HttpServletResponse httpServletResponse) {
        try {
            final PowerJobUser powerJobUser = powerJobAuthService.tryLogin(loginContext);
            if (powerJobUser == null) {
                return ResultDTO.failed("USER_NOT_FOUND");
            }
            httpServletResponse.addCookie(new Cookie("power_jwt", powerJobUser.getJwtToken()));
            return ResultDTO.success(powerJobUser);
        } catch (Exception e) {
            return ResultDTO.failed(e.getMessage());
        }
    }

    /**
     * 第三方账号体系回调登录接口
     * @param httpServletRequest 请求
     * @param httpServletResponse 响应
     * @return 登录结果
     */
    @RequestMapping(value = "/loginCallback", method = {RequestMethod.GET, RequestMethod.POST})
    public ResultDTO<PowerJobUser> loginCallback(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        LoginContext loginContext = new LoginContext()
                .setHttpServletRequest(httpServletRequest);

        // 常见登录组件的标准规范（钉钉、企业微信、飞书），第三方原样透传
        final String state = httpServletRequest.getParameter("state");
        loginContext.setLoginType(state);

        final PowerJobUser powerJobUser = powerJobAuthService.tryLogin(loginContext);

        httpServletResponse.addCookie(new Cookie("power_jwt", powerJobUser.getJwtToken()));
        return ResultDTO.success(powerJobUser);
    }
}
