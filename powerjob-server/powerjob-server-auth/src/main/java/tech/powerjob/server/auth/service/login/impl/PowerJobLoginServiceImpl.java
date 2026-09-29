package tech.powerjob.server.auth.service.login.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.server.auth.LoginUserHolder;
import tech.powerjob.server.auth.PowerJobUser;
import tech.powerjob.server.auth.common.AuthConstants;
import tech.powerjob.common.enums.ErrorCodes;
import tech.powerjob.server.auth.common.PowerJobAuthException;
import tech.powerjob.server.auth.common.utils.HttpServletUtils;
import tech.powerjob.server.auth.jwt.JwtService;
import tech.powerjob.server.auth.login.*;
import tech.powerjob.server.auth.service.login.LoginRequest;
import tech.powerjob.server.auth.service.login.PowerJobLoginService;
import tech.powerjob.server.common.Loggers;
import tech.powerjob.common.enums.SwitchableStatus;
import tech.powerjob.server.persistence.remote.model.UserInfoDO;
import tech.powerjob.server.persistence.remote.repository.UserInfoRepository;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Date;
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
    public String fetchThirdPartyLoginUrl(String type, HttpServletRequest httpServletRequest) {
        final ThirdPartyLoginService thirdPartyLoginService = fetchBizLoginService(type);
        return thirdPartyLoginService.generateLoginUrl(httpServletRequest);
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
            // 写入账号体系类型
            newUser.setAccountType(loginType);
            newUser.setOriginUsername(bizUser.getUsername());

            newUser.setTokenLoginVerifyInfo(JsonUtils.toJSONString(bizUser.getTokenLoginVerifyInfo()));

            // 同步素材
            newUser.setEmail(bizUser.getEmail());
            newUser.setPhone(bizUser.getPhone());
            newUser.setNick(bizUser.getNick());
            newUser.setWebHook(bizUser.getWebHook());
            newUser.setExtra(bizUser.getExtra());

            Loggers.WEB.info("[PowerJobLoginService] sync user to PowerJobUserSystem: {}", dbUserName);
            userInfoRepository.saveAndFlush(newUser);

            powerJobUserOpt = userInfoRepository.findByUsername(dbUserName);
        } else {

            UserInfoDO dbUserInfoDO = powerJobUserOpt.get();

            checkUserStatus(dbUserInfoDO);

            // 更新二次校验的 TOKEN 信息
            dbUserInfoDO.setTokenLoginVerifyInfo(JsonUtils.toJSONString(bizUser.getTokenLoginVerifyInfo()));
            dbUserInfoDO.setGmtModified(new Date());

            userInfoRepository.saveAndFlush(dbUserInfoDO);
        }

        PowerJobUser ret = new PowerJobUser();

        // 理论上 100% 存在
        if (powerJobUserOpt.isPresent()) {
            final UserInfoDO dbUser = powerJobUserOpt.get();
            BeanUtils.copyProperties(dbUser, ret);
            ret.setUsername(dbUserName);
        }

        fillJwt(ret, Optional.ofNullable(bizUser.getTokenLoginVerifyInfo()).map(TokenLoginVerifyInfo::getEncryptedToken).orElse(null));

        return ret;
    }

    @Override
    public Optional<PowerJobUser> ifLogin(HttpServletRequest httpServletRequest) {
        final Optional<JwtBody> jwtBodyOpt = parseJwt(httpServletRequest);
        if (!jwtBodyOpt.isPresent()) {
            return Optional.empty();
        }

        JwtBody jwtBody = jwtBodyOpt.get();

        Optional<UserInfoDO> dbUserInfoOpt = userInfoRepository.findByUsername(jwtBody.getUsername());
        if (!dbUserInfoOpt.isPresent()) {
            throw new PowerJobAuthException(ErrorCodes.USER_NOT_EXIST);
        }

        UserInfoDO dbUser = dbUserInfoOpt.get();

        checkUserStatus(dbUser);

        PowerJobUser powerJobUser = new PowerJobUser();

        String tokenLoginVerifyInfoStr = dbUser.getTokenLoginVerifyInfo();
        TokenLoginVerifyInfo tokenLoginVerifyInfo = Optional.ofNullable(tokenLoginVerifyInfoStr).map(x -> JsonUtils.parseObjectIgnoreException(x, TokenLoginVerifyInfo.class)).orElse(new TokenLoginVerifyInfo());

        // DB 中的 encryptedToken 存在，代表需要二次校验
        if (StringUtils.isNotEmpty(tokenLoginVerifyInfo.getEncryptedToken())) {
            if (!StringUtils.equals(jwtBody.getEncryptedToken(), tokenLoginVerifyInfo.getEncryptedToken())) {
                throw new PowerJobAuthException(ErrorCodes.INVALID_TOKEN);
            }

            ThirdPartyLoginService thirdPartyLoginService = code2ThirdPartyLoginService.get(dbUser.getAccountType());
            boolean tokenLoginVerifyOk = thirdPartyLoginService.tokenLoginVerify(dbUser.getOriginUsername(), tokenLoginVerifyInfo);

            if (!tokenLoginVerifyOk) {
                throw new PowerJobAuthException(ErrorCodes.USER_AUTH_FAILED);
            }
        }

        BeanUtils.copyProperties(dbUser, powerJobUser);

        // 兼容某些直接通过 ifLogin 判断登录的场景
        LoginUserHolder.set(powerJobUser);

        return Optional.of(powerJobUser);
    }

    /**
     * 检查 user 状态
     * @param dbUser user
     */
    private void checkUserStatus(UserInfoDO dbUser) {
        int accountStatus = Optional.ofNullable(dbUser.getStatus()).orElse(SwitchableStatus.ENABLE.getV());
        if (accountStatus == SwitchableStatus.DISABLE.getV()) {
            throw new PowerJobAuthException(ErrorCodes.USER_DISABLED);
        }
    }

    private ThirdPartyLoginService fetchBizLoginService(String loginType) {
        final ThirdPartyLoginService loginService = code2ThirdPartyLoginService.get(loginType);
        if (loginService == null) {
            throw new PowerJobAuthException(ErrorCodes.INVALID_REQUEST, "can't find ThirdPartyLoginService by type: " + loginType);
        }
        return loginService;
    }

    private void fillJwt(PowerJobUser powerJobUser, String encryptedToken) {

        // 不能下发 userId，容易被轮询爆破
        JwtBody jwtBody = new JwtBody();
        jwtBody.setUsername(powerJobUser.getUsername());
        if (StringUtils.isNotEmpty(encryptedToken)) {
            jwtBody.setEncryptedToken(encryptedToken);
        }

        Map<String, Object> jwtMap = JsonUtils.parseMap(JsonUtils.toJSONString(jwtBody));

        powerJobUser.setJwtToken(jwtService.build(jwtMap, null));
    }

    @SneakyThrows
    private Optional<JwtBody> parseJwt(HttpServletRequest httpServletRequest) {
        // header、cookie 都能获取
        String jwtStr = HttpServletUtils.fetchFromHeader(AuthConstants.JWT_NAME, httpServletRequest);
        if (StringUtils.isEmpty(jwtStr)) {
            jwtStr = HttpServletUtils.fetchFromHeader(AuthConstants.OLD_JWT_NAME, httpServletRequest);
        }

        /*

        开发阶段跨域无法简单传输 cookies，暂时采取 header 方案传输 JWT

        if (StringUtils.isEmpty(jwtStr)) {
            for (Cookie cookie : Optional.ofNullable(httpServletRequest.getCookies()).orElse(new Cookie[]{})) {
                if (cookie.getName().equals(AuthConstants.JWT_NAME)) {
                    jwtStr = cookie.getValue();
                }
            }
        }
         */

        if (StringUtils.isEmpty(jwtStr)) {
            return Optional.empty();
        }
        final Map<String, Object> jwtBodyMap = jwtService.parse(jwtStr, null).getResult();

        if (MapUtils.isEmpty(jwtBodyMap)) {
            return Optional.empty();
        }

        return Optional.ofNullable(JsonUtils.parseObject(JsonUtils.toJSONString(jwtBodyMap), JwtBody.class));
    }

    @Data
    static class JwtBody implements Serializable {

        private String username;

        private String encryptedToken;
    }
}
