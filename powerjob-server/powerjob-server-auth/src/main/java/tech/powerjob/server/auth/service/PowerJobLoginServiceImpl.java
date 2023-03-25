package tech.powerjob.server.auth.service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tech.powerjob.common.Loggers;
import tech.powerjob.server.auth.LoginContext;
import tech.powerjob.server.auth.Permission;
import tech.powerjob.server.auth.PowerJobUser;
import tech.powerjob.server.auth.Role;
import tech.powerjob.server.auth.anno.ApiPermission;
import tech.powerjob.server.auth.login.BizLoginService;
import tech.powerjob.server.auth.login.BizUser;
import tech.powerjob.server.persistence.remote.model.UserInfoDO;
import tech.powerjob.server.persistence.remote.model.UserRoleDO;
import tech.powerjob.server.persistence.remote.repository.UserInfoRepository;
import tech.powerjob.server.persistence.remote.repository.UserRoleRepository;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * PowerJobLoginService
 *
 * @author tjq
 * @since 2023/3/21
 */
@Service
public class PowerJobLoginServiceImpl implements PowerJobAuthService {

    private final UserInfoRepository userInfoRepository;
    private final UserRoleRepository userRoleRepository;
    private final Map<String, BizLoginService> type2LoginService = Maps.newHashMap();

    private static final String TOKEN_HEADER_NAME = "power_token";

    @Autowired
    public PowerJobLoginServiceImpl(List<BizLoginService> loginServices, UserInfoRepository userInfoRepository, UserRoleRepository userRoleRepository) {
        this.userInfoRepository = userInfoRepository;
        this.userRoleRepository = userRoleRepository;
        loginServices.forEach(k -> type2LoginService.put(k.type(), k));
    }


    @Override
    public Optional<PowerJobUser> login(LoginContext loginContext) {
        final String loginType = loginContext.getLoginType();
        final BizLoginService loginService = type2LoginService.get(loginType);
        if (loginService == null) {
            throw new IllegalArgumentException("can't find LoginService by type: " + loginType);
        }
        final Optional<BizUser> bizUserOpt = loginService.login(loginContext);
        if (!bizUserOpt.isPresent()) {
            return Optional.empty();
        }
        final BizUser bizUser = bizUserOpt.get();

        String dbUserName = String.format("%s_%s", loginType, bizUser.getUsername());
        final Optional<UserInfoDO> powerJobUserOpt = userInfoRepository.findByUsername(dbUserName);

        PowerJobUser ret = new PowerJobUser();
        // 存在则响应 PowerJob 用户
        if (powerJobUserOpt.isPresent()) {
            final UserInfoDO dbUser = powerJobUserOpt.get();
            BeanUtils.copyProperties(dbUser, ret);
            ret.setUsername(dbUserName);
            return Optional.of(ret);
        }

        // 同步在 PowerJob 用户库创建该用户
        UserInfoDO newUser = new UserInfoDO();
        newUser.setUsername(dbUserName);
        Loggers.WEB.info("[PowerJobLoginService] sync user to PowerJobUserSystem: {}", dbUserName);
        userInfoRepository.saveAndFlush(newUser);
        ret.setUsername(dbUserName);

        return Optional.of(ret);
    }

    @Override
    public Optional<PowerJobUser> parse(HttpServletRequest httpServletRequest) {

        final Optional<String> usernameOpt = parseUsername(httpServletRequest);
        if (!usernameOpt.isPresent()) {
            return Optional.empty();
        }
        return userInfoRepository.findByUsername(usernameOpt.get()).map(userInfoDO -> {
            PowerJobUser powerJobUser = new PowerJobUser();
            BeanUtils.copyProperties(usernameOpt, powerJobUser);
            return powerJobUser;
        });
    }

    @Override
    public boolean hasPermission(HttpServletRequest request, PowerJobUser user, ApiPermission apiPermission) {

        final List<UserRoleDO> userRoleList = Optional.ofNullable(userRoleRepository.findAllByUserId(user.getId())).orElse(Collections.emptyList());

        Multimap<String, Role> appId2Role = ArrayListMultimap.create();

        for (UserRoleDO userRole : userRoleList) {
            if (userRole.getRole().equalsIgnoreCase(String.valueOf(Role.GOD.getV()))) {
                return true;
            }

            // 除了上帝角色，其他任何角色都是 roleId_appId 的形式
            final String[] split = userRole.getRole().split("_");
            final Role role = Role.of(Integer.parseInt(split[0]));
            appId2Role.put(split[1], role);
        }

        // 无超级管理员权限，校验普通权限
        final String appId = request.getHeader("appId");
        if (StringUtils.isEmpty(appId)) {
            throw new IllegalArgumentException("can't find appId in header, please login again!");
        }

        final Permission requiredPermission = apiPermission.requiredPermission();

        final Collection<Role> roleCollection = appId2Role.get(appId);
        for (Role role : roleCollection) {
            if (role.getPermissions().contains(requiredPermission)) {
                return true;
            }
        }

        return false;
    }

    private Optional<String> parseUsername(HttpServletRequest httpServletRequest) {
        final String tokenHeader = httpServletRequest.getHeader(TOKEN_HEADER_NAME);
        if (StringUtils.isEmpty(tokenHeader)) {
            return Optional.empty();
        }

        // TODO: 从 jwt token 解析 username
        return Optional.empty();
    }
}
