package tech.powerjob.server.auth.service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tech.powerjob.common.Loggers;
import tech.powerjob.common.exception.ImpossibleException;
import tech.powerjob.server.auth.*;
import tech.powerjob.server.auth.interceptor.ApiPermission;
import tech.powerjob.server.auth.interceptor.dp.DynamicPermission;
import tech.powerjob.server.auth.interceptor.dp.EmptyDynamicPermission;
import tech.powerjob.server.auth.jwt.JwtService;
import tech.powerjob.server.auth.login.BizLoginService;
import tech.powerjob.server.auth.login.BizUser;
import tech.powerjob.server.persistence.remote.model.AppInfoDO;
import tech.powerjob.server.persistence.remote.model.UserInfoDO;
import tech.powerjob.server.persistence.remote.model.UserRoleDO;
import tech.powerjob.server.persistence.remote.repository.AppInfoRepository;
import tech.powerjob.server.persistence.remote.repository.UserInfoRepository;
import tech.powerjob.server.persistence.remote.repository.UserRoleRepository;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * PowerJobLoginService
 *
 * @author tjq
 * @since 2023/3/21
 */
@Slf4j
@Service
public class PowerJobAuthServiceImpl implements PowerJobAuthService {

    private final JwtService jwtService;
    private final AppInfoRepository appInfoRepository;
    private final UserInfoRepository userInfoRepository;
    private final UserRoleRepository userRoleRepository;
    private final Map<String, BizLoginService> type2LoginService = Maps.newHashMap();

    private static final String JWT_NAME = "power_jwt";

    private static final String KEY_USERNAME = "userName";

    @Autowired
    public PowerJobAuthServiceImpl(List<BizLoginService> loginServices, JwtService jwtService, AppInfoRepository appInfoRepository, UserInfoRepository userInfoRepository, UserRoleRepository userRoleRepository) {
        this.jwtService = jwtService;
        this.appInfoRepository = appInfoRepository;
        this.userInfoRepository = userInfoRepository;
        this.userRoleRepository = userRoleRepository;
        loginServices.forEach(k -> type2LoginService.put(k.type(), k));
    }


    @Override
    public List<String> supportTypes() {
        return Lists.newArrayList(type2LoginService.keySet());
    }

    @Override
    public String startLogin(LoginContext loginContext) {
        final BizLoginService loginService = fetchBizLoginService(loginContext);
        return loginService.loginUrl();
    }

    @Override
    public PowerJobUser tryLogin(LoginContext loginContext) {
        final String loginType = loginContext.getLoginType();
        final BizLoginService loginService = fetchBizLoginService(loginContext);
        final BizUser bizUser = loginService.login(loginContext);

        String dbUserName = String.format("%s_%s", loginType, bizUser.getUsername());
        Optional<UserInfoDO> powerJobUserOpt = userInfoRepository.findByUsername(dbUserName);

        // 如果不存在用户，先同步创建用户
        if (!powerJobUserOpt.isPresent()) {
            UserInfoDO newUser = new UserInfoDO();
            newUser.setUsername(dbUserName);
            Loggers.WEB.info("[PowerJobLoginService] sync user to PowerJobUserSystem: {}", dbUserName);
            userInfoRepository.saveAndFlush(newUser);

            powerJobUserOpt = userInfoRepository.findByUsername(dbUserName);
        }

        PowerJobUser ret = new PowerJobUser();

        // 理论上 100% 存在
        if (powerJobUserOpt.isPresent()) {
            final UserInfoDO dbUser = powerJobUserOpt.get();
            BeanUtils.copyProperties(dbUser, ret);
            ret.setUsername(dbUserName);
        }

        fillJwt(ret);

        return ret;
    }

    @Override
    public Optional<PowerJobUser> ifLogin(HttpServletRequest httpServletRequest) {

        final Optional<String> userNameOpt = parseUserName(httpServletRequest);
        return userNameOpt.flatMap(uname -> userInfoRepository.findByUsername(uname).map(userInfoDO -> {
            PowerJobUser powerJobUser = new PowerJobUser();
            BeanUtils.copyProperties(userInfoDO, powerJobUser);
            return powerJobUser;
        }));
    }

    @Override
    public boolean hasPermission(HttpServletRequest request, Object handler, PowerJobUser user, ApiPermission apiPermission) {

        final List<UserRoleDO> userRoleList = Optional.ofNullable(userRoleRepository.findAllByUserId(user.getId())).orElse(Collections.emptyList());

        Multimap<Long, Role> appId2Role = ArrayListMultimap.create();
        Multimap<Long, Role> namespaceId2Role = ArrayListMultimap.create();

        for (UserRoleDO userRole : userRoleList) {
            if (Objects.equals(Role.GOD.getV(), userRole.getRole())) {
                return true;
            }

            final Role role = Role.of(userRole.getRole());
            if (Objects.equals(userRole.getScope(), RoleScope.NAMESPACE.getV())) {
                namespaceId2Role.put(userRole.getTarget(), role);
            }
            if (Objects.equals(userRole.getScope(), RoleScope.APP.getV())) {
                appId2Role.put(userRole.getTarget(), role);
            }
        }

        // 无超级管理员权限，校验普通权限
        final String appIdStr = request.getHeader("appId");
        if (StringUtils.isEmpty(appIdStr)) {
            throw new IllegalArgumentException("can't find appId in header, please refresh and try again!");
        }

        Long appId = Long.valueOf(appIdStr);

        final Permission requiredPermission = parsePermission(request, handler, apiPermission);
        if (requiredPermission == Permission.NONE) {
            return true;
        }

        final Collection<Role> appRoles = appId2Role.get(appId);
        for (Role role : appRoles) {
            if (role.getPermissions().contains(requiredPermission)) {
                return true;
            }
        }

        // 校验 namespace 穿透权限
        Optional<AppInfoDO> appInfoOpt = appInfoRepository.findById(appId);
        if (!appInfoOpt.isPresent()) {
            throw new IllegalArgumentException("can't find appInfo by appId in permission check: " + appId);
        }
        Long namespaceId = Optional.ofNullable(appInfoOpt.get().getNamespaceId()).orElse(-1L);
        Collection<Role> namespaceRoles = namespaceId2Role.get(namespaceId);
        for (Role role : namespaceRoles) {
            if (role.getPermissions().contains(requiredPermission)) {
                return true;
            }
        }

        return false;
    }

    private Optional<String> parseUserName(HttpServletRequest httpServletRequest) {
        // header、cookie 都能获取
        String jwtStr = httpServletRequest.getHeader(JWT_NAME);
        if (StringUtils.isEmpty(jwtStr)) {
            for (Cookie cookie : httpServletRequest.getCookies()) {
                if (cookie.getName().equals(JWT_NAME)) {
                    jwtStr = cookie.getValue();
                }
            }
        }
        if (StringUtils.isEmpty(jwtStr)) {
            return Optional.empty();
        }
        final Map<String, Object> jwtBodyMap = jwtService.parse(jwtStr, null);
        final Object userName = jwtBodyMap.get(KEY_USERNAME);

        if (userName == null) {
            return Optional.empty();
        }

        return Optional.of(String.valueOf(userName));
    }

    private BizLoginService fetchBizLoginService(LoginContext loginContext) {
        final String loginType = loginContext.getLoginType();
        final BizLoginService loginService = type2LoginService.get(loginType);
        if (loginService == null) {
            throw new IllegalArgumentException("can't find LoginService by type: " + loginType);
        }
        return loginService;
    }

    private void fillJwt(PowerJobUser powerJobUser) {
        Map<String, Object> jwtMap = Maps.newHashMap();

        // 不能下发 userId，容易被轮询爆破
        jwtMap.put(KEY_USERNAME, powerJobUser.getUsername());

        powerJobUser.setJwtToken(jwtService.build(jwtMap, null));
    }

    private static Permission parsePermission(HttpServletRequest request, Object handler, ApiPermission apiPermission) {
        Class<? extends DynamicPermission> dynamicPermissionPlugin = apiPermission.dynamicPermissionPlugin();
        if (EmptyDynamicPermission.class.equals(dynamicPermissionPlugin)) {
            return apiPermission.requiredPermission();
        }
        try {
            DynamicPermission dynamicPermission = dynamicPermissionPlugin.getDeclaredConstructor().newInstance();
            return dynamicPermission.calculate(request, handler);
        } catch (Throwable t) {
            log.error("[PowerJobAuthService] process dynamicPermissionPlugin failed!", t);
            ExceptionUtils.rethrow(t);
        }
        throw new ImpossibleException();
    }
}
