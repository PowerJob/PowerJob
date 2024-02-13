package tech.powerjob.server.auth.login.impl;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.server.auth.common.AuthConstants;
import tech.powerjob.server.auth.common.AuthErrorCode;
import tech.powerjob.server.auth.common.PowerJobAuthException;
import tech.powerjob.server.auth.login.LoginTypeInfo;
import tech.powerjob.server.auth.login.ThirdPartyLoginRequest;
import tech.powerjob.server.auth.login.ThirdPartyLoginService;
import tech.powerjob.server.auth.login.ThirdPartyUser;
import tech.powerjob.server.common.Loggers;
import tech.powerjob.server.common.utils.DigestUtils;
import tech.powerjob.server.persistence.remote.model.PwjbUserInfoDO;
import tech.powerjob.server.persistence.remote.repository.PwjbUserInfoRepository;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;

/**
 * PowerJob 自带的登陆服务
 * 和应用主框架无关，依然属于第三方登录体系
 *
 * @author tjq
 * @since 2023/3/20
 */
@Service
public class PowerJobThirdPartyLoginService implements ThirdPartyLoginService {

    @Resource
    private PwjbUserInfoRepository pwjbUserInfoRepository;

    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";

    private static final String KEY_ENCRYPTION = "encryption";

    @Override
    public LoginTypeInfo loginType() {
        return new LoginTypeInfo()
                .setType(AuthConstants.ACCOUNT_TYPE_POWER_JOB)
                .setName("PowerJob Built-in Login")
                ;
    }

    @Override
    public String generateLoginUrl(HttpServletRequest httpServletRequest) {
        // 前端实现跳转，服务端返回特殊指令
        return AuthConstants.FE_REDIRECT_KEY.concat("powerjobLogin");
    }

    @Override
    public ThirdPartyUser login(ThirdPartyLoginRequest loginRequest) {
        final String loginInfo = loginRequest.getOriginParams();
        if (StringUtils.isEmpty(loginInfo)) {
            throw new IllegalArgumentException("can't find login Info");
        }

        Map<String, Object> loginInfoMap = JsonUtils.parseMap(loginInfo);

        final String username = MapUtils.getString(loginInfoMap, KEY_USERNAME);
        final String password = MapUtils.getString(loginInfoMap, KEY_PASSWORD);
        final String encryption = MapUtils.getString(loginInfoMap, KEY_ENCRYPTION);

        if (StringUtils.isAnyEmpty(username, password)) {
            Loggers.WEB.debug("[PowerJobLoginService] username or password is empty, login failed!");
            throw new PowerJobAuthException(AuthErrorCode.INVALID_REQUEST);
        }

        final Optional<PwjbUserInfoDO> userInfoOpt = pwjbUserInfoRepository.findByUsername(username);
        if (!userInfoOpt.isPresent()) {
            Loggers.WEB.debug("[PowerJobLoginService] can't find user by username: {}", username);
            throw new PowerJobAuthException(AuthErrorCode.USER_NOT_EXIST);
        }

        final PwjbUserInfoDO dbUser = userInfoOpt.get();

        if (DigestUtils.rePassword(password, username).equals(dbUser.getPassword())) {
            ThirdPartyUser bizUser = new ThirdPartyUser();
            bizUser.setUsername(username);

            // 回填第一次创建的信息
            String extra = dbUser.getExtra();
            if (StringUtils.isNotEmpty(extra)) {
                ThirdPartyUser material = JsonUtils.parseObjectIgnoreException(extra, ThirdPartyUser.class);
                if (material != null) {
                    bizUser.setEmail(material.getEmail());
                    bizUser.setNick(material.getNick());
                    bizUser.setPhone(material.getPhone());
                    bizUser.setWebHook(material.getWebHook());
                }
            }

            return bizUser;
        }

        Loggers.WEB.debug("[PowerJobLoginService] user[{}]'s password is incorrect, login failed!", username);
        throw new PowerJobException("password is incorrect");
    }
}
