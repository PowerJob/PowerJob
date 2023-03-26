package tech.powerjob.server.web.controller;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import tech.powerjob.common.response.ResultDTO;
import tech.powerjob.server.auth.LoginContext;
import tech.powerjob.server.auth.PowerJobUser;
import tech.powerjob.server.auth.service.PowerJobAuthService;
import tech.powerjob.server.core.service.UserService;
import tech.powerjob.server.persistence.remote.model.UserInfoDO;
import tech.powerjob.server.persistence.remote.repository.UserInfoRepository;
import tech.powerjob.server.web.request.ModifyUserInfoRequest;
import tech.powerjob.server.web.request.UserLoginRequest;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户信息控制层
 *
 * @author tjq
 * @since 2020/4/12
 */
@RestController
@RequestMapping("/user")
public class UserInfoController {
    @Resource
    private UserService userService;
    @Resource
    private UserInfoRepository userInfoRepository;
    @Resource
    private PowerJobAuthService powerJobAuthService;


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

    @RequestMapping(value = "/loginCallback", method = {RequestMethod.GET, RequestMethod.POST})
    public ResultDTO<PowerJobUser> loginCallback(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        LoginContext loginContext = new LoginContext()
                .setHttpServletRequest(httpServletRequest);

        // 尝试读取 body
        if (RequestMethod.POST.name().equalsIgnoreCase(httpServletRequest.getMethod())) {
            // TODO: 从 post 读取 body
        }

        // 钉钉回调
        final String state = httpServletRequest.getParameter("state");
        if ("DingTalk".equalsIgnoreCase(state)) {
            loginContext.setLoginType("DingTalk");
        }

        final PowerJobUser powerJobUser = powerJobAuthService.tryLogin(loginContext);

        httpServletResponse.addCookie(new Cookie("powerjob_token", powerJobUser.getJwtToken()));
        return ResultDTO.success(powerJobUser);
    }

    @PostMapping("save")
    public ResultDTO<Void> save(@RequestBody ModifyUserInfoRequest request) {
        UserInfoDO userInfoDO = new UserInfoDO();
        BeanUtils.copyProperties(request, userInfoDO);
        userService.save(userInfoDO);
        return ResultDTO.success(null);
    }

    @GetMapping("list")
    public ResultDTO<List<UserItemVO>> list(@RequestParam(required = false) String name) {

        List<UserInfoDO> result;
        if (StringUtils.isEmpty(name)) {
            result = userInfoRepository.findAll();
        }else {
            result = userInfoRepository.findByUsernameLike("%" + name + "%");
        }
        return ResultDTO.success(convert(result));
    }

    private static List<UserItemVO> convert(List<UserInfoDO> data) {
        if (CollectionUtils.isEmpty(data)) {
            return Lists.newLinkedList();
        }
        return data.stream().map(x -> new UserItemVO(x.getId(), x.getUsername())).collect(Collectors.toList());
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class UserItemVO {
        private Long id;
        private String username;
    }
}
