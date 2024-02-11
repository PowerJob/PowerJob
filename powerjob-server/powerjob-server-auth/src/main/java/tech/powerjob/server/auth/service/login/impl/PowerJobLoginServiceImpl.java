package tech.powerjob.server.auth.service.login.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tech.powerjob.server.auth.PowerJobUser;
import tech.powerjob.server.auth.common.AuthErrorCode;
import tech.powerjob.server.auth.common.PowerJobAuthException;
import tech.powerjob.server.auth.jwt.JwtService;
import tech.powerjob.server.auth.login.LoginTypeInfo;
import tech.powerjob.server.auth.login.ThirdPartyLoginRequest;
import tech.powerjob.server.auth.login.ThirdPartyLoginService;
import tech.powerjob.server.auth.login.ThirdPartyUser;
import tech.powerjob.server.auth.service.login.LoginRequest;
import tech.powerjob.server.auth.service.login.PowerJobLoginService;
import tech.powerjob.server.common.Loggers;
import tech.powerjob.server.persistence.remote.model.UserInfoDO;
import tech.powerjob.server.persistence.remote.repository.UserInfoRepository;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * PowerJob 登录服务
 *
 * @author tjq
 * @since 2024/2/10
 */
@Slf4j
@Service
public class PowerJobLoginServiceImpl implements PowerJobLoginService {


    private final JwtService jwtService;
    private final UserInfoRepository userInfoRepository;
    private final Map<String, ThirdPartyLoginService> code2ThirdPartyLoginService;

    private static final String JWT_NAME = "power_jwt";

    private static final String KEY_USERNAME = "userName";

    @Autowired
    public PowerJobLoginServiceImpl(JwtService jwtService, UserInfoRepository userInfoRepository, List<ThirdPartyLoginService> thirdPartyLoginServices) {

        this.jwtService = jwtService;
        this.userInfoRepository = userInfoRepository;

        code2ThirdPartyLoginService = Maps.newHashMap();
        thirdPartyLoginServices.forEach(s -> {
            code2ThirdPartyLoginService.put(s.loginType().getType(), s);
            log.info("[PowerJobLoginService] register ThirdPartyLoginService: {}", s.loginType());
        });
    }

    @Override
    public List<LoginTypeInfo> fetchSupportLoginTypes() {
        return Lists.newArrayList(code2ThirdPartyLoginService.values()).stream().map(ThirdPartyLoginService::loginType).collect(Collectors.toList());
    }

    @Override
    public String fetchThirdPartyLoginUrl(HttpServletRequest httpServletRequest) {
        return null;
    }

    @Override
    public PowerJobUser doLogin(LoginRequest loginRequest) throws PowerJobAuthException {
        final String loginType = loginRequest.getLoginType();
        final ThirdPartyLoginService thirdPartyLoginService = fetchBizLoginService(loginType);

        ThirdPartyLoginRequest thirdPartyLoginRequest = new ThirdPartyLoginRequest()
                .setOriginParams(loginRequest.getOriginParams())
                .setHttpServletRequest(loginRequest.getHttpServletRequest());

        final ThirdPartyUser bizUser = thirdPartyLoginService.login(thirdPartyLoginRequest);

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

    private ThirdPartyLoginService fetchBizLoginService(String loginType) {
        final ThirdPartyLoginService loginService = code2ThirdPartyLoginService.get(loginType);
        if (loginService == null) {
            throw new PowerJobAuthException(AuthErrorCode.INVALID_REQUEST, "can't find ThirdPartyLoginService by type: " + loginType);
        }
        return loginService;
    }

    private void fillJwt(PowerJobUser powerJobUser) {
        Map<String, Object> jwtMap = Maps.newHashMap();

        // 不能下发 userId，容易被轮询爆破
        jwtMap.put(KEY_USERNAME, powerJobUser.getUsername());

        powerJobUser.setJwtToken(jwtService.build(jwtMap, null));
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
}
