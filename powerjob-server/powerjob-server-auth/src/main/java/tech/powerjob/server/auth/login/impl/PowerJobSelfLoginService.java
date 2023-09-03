package tech.powerjob.server.auth.login.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import tech.powerjob.common.Loggers;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.utils.DigestUtils;
import tech.powerjob.server.auth.LoginContext;
import tech.powerjob.server.auth.login.BizLoginService;
import tech.powerjob.server.auth.login.BizUser;
import tech.powerjob.server.common.SJ;
import tech.powerjob.server.persistence.remote.model.UserInfoDO;
import tech.powerjob.server.persistence.remote.repository.UserInfoRepository;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Optional;

/**
 * PowerJob 自带的登陆服务
 *
 * @author tjq
 * @since 2023/3/20
 */
@Service
public class PowerJobSelfLoginService implements BizLoginService {

    @Resource
    private UserInfoRepository userInfoRepository;

    public static final String DEFAULT_LOGIN_SERVICE = "PowerJob";

    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";

    @Override
    public String type() {
        return DEFAULT_LOGIN_SERVICE;
    }

    @Override
    public String loginUrl() {
        return null;
    }

    @Override
    public BizUser login(LoginContext loginContext) {

        final String loginInfo = loginContext.getLoginInfo();
        if (StringUtils.isEmpty(loginInfo)) {
            throw new IllegalArgumentException("can't find login Info");
        }

        final Map<String, String> loginInfoMap = SJ.splitKvString(loginInfo);
        final String username = loginInfoMap.get(KEY_USERNAME);
        final String password = loginInfoMap.get(KEY_PASSWORD);

        if (StringUtils.isAnyEmpty(username, password)) {
            Loggers.WEB.debug("[DefaultBizLoginService] username or password is empty, login failed!");
            throw new IllegalArgumentException("username or password is empty!");
        }

        final Optional<UserInfoDO> userInfoOpt = userInfoRepository.findByUsername(username);
        if (!userInfoOpt.isPresent()) {
            Loggers.WEB.debug("[DefaultBizLoginService] can't find user by username: {}", username);
            throw new PowerJobException("can't find user by username: " + username);
        }

        final UserInfoDO dbUser = userInfoOpt.get();

        if (DigestUtils.rePassword(password, username).equals(dbUser.getPassword())) {
            BizUser bizUser = new BizUser();
            bizUser.setUsername(username);
            return bizUser;
        }

        Loggers.WEB.debug("[DefaultBizLoginService] user[{}]'s password is incorrect, login failed!", username);
        throw new PowerJobException("password is incorrect");
    }
}
