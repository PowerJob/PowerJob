package tech.powerjob.server.web.controller;

import org.springframework.web.bind.annotation.*;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.server.auth.PowerJobUser;
import tech.powerjob.server.auth.common.AuthConstants;
import tech.powerjob.server.auth.login.LoginTypeInfo;
import tech.powerjob.server.auth.service.login.LoginRequest;
import tech.powerjob.server.auth.service.login.PowerJobLoginService;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;

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
    private PowerJobLoginService powerJobLoginService;

    @GetMapping("/supportLoginTypes")
    public ResultDTO<List<LoginTypeInfo>> listSupportLoginTypes() {
        return ResultDTO.success(powerJobLoginService.fetchSupportLoginTypes());
    }

    @GetMapping("/thirdPartyLoginUrl")
    public ResultDTO<String> getThirdPartyLoginUrl(String type, HttpServletRequest request) {
        String url = powerJobLoginService.fetchThirdPartyLoginUrl(type, request);
        return ResultDTO.success(url);
    }

    /**
     * 第三方账号体系回调登录接口，eg, 接受钉钉登录回调
     * @param httpServletRequest 请求
     * @param httpServletResponse 响应
     * @return 登录结果
     */
    @RequestMapping(value = "/thirdPartyLoginCallback", method = {RequestMethod.GET, RequestMethod.POST})
    public ResultDTO<PowerJobUser> loginCallback(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {

        LoginRequest loginContext = new LoginRequest().setHttpServletRequest(httpServletRequest);

        // 常见登录组件的标准规范（钉钉、企业微信、飞书），第三方原样透传。开发者在对接第三方登录体系时，可能需要修改此处，将 type 回填
        final String state = httpServletRequest.getParameter("state");
        loginContext.setLoginType(state);

        final PowerJobUser powerJobUser = powerJobLoginService.doLogin(loginContext);
        fillJwt4LoginUser(powerJobUser, httpServletResponse);

        return ResultDTO.success(powerJobUser);
    }

    /**
     * 第三方账号体系直接登录接口，eg, 接受 PowerJob 自带账号密码体系的登录请求
     * @param loginRequest 登录请求
     * @param httpServletResponse 响应
     * @return 登录结果
     */
    @PostMapping("/thirdPartyLoginDirect")
    public ResultDTO<PowerJobUser> selfLogin(@RequestBody LoginRequest loginRequest, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        loginRequest.setHttpServletRequest(httpServletRequest);
        try {
            final PowerJobUser powerJobUser = powerJobLoginService.doLogin(loginRequest);
            if (powerJobUser == null) {
                return ResultDTO.failed("USER_NOT_FOUND");
            }
            fillJwt4LoginUser(powerJobUser, httpServletResponse);
            return ResultDTO.success(powerJobUser);
        } catch (Exception e) {
            return ResultDTO.failed(e.getMessage());
        }
    }

    @GetMapping(value = "/ifLogin")
    public ResultDTO<PowerJobUser> ifLogin(HttpServletRequest httpServletRequest) {
        final Optional<PowerJobUser> powerJobUser = powerJobLoginService.ifLogin(httpServletRequest);
        return powerJobUser.map(ResultDTO::success).orElseGet(() -> ResultDTO.success(null));
    }

    private void fillJwt4LoginUser(PowerJobUser powerJobUser, HttpServletResponse httpServletResponse) {
        httpServletResponse.addCookie(new Cookie(AuthConstants.JWT_NAME, powerJobUser.getJwtToken()));
    }
}
