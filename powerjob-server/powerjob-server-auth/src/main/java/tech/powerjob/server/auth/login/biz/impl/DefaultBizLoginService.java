package tech.powerjob.server.auth.login.biz.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import tech.powerjob.common.utils.DigestUtils;
import tech.powerjob.server.auth.login.LoginContext;
import tech.powerjob.server.auth.login.biz.BizLoginService;
import tech.powerjob.server.auth.login.biz.BizUser;
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
@Slf4j
@Service
public class DefaultBizLoginService implements BizLoginService {

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
    public Optional<BizUser> login(LoginContext loginContext) {

        final String loginInfo = loginContext.getLoginInfo();
        if (StringUtils.isEmpty(loginInfo)) {
            return Optional.empty();
        }

        final Map<String, String> loginInfoMap = SJ.splitKvString(loginInfo);
        final String username = loginInfoMap.get(KEY_USERNAME);
        final String password = loginInfoMap.get(KEY_PASSWORD);

        if (StringUtils.isAnyEmpty(username, password)) {
            log.debug("[DefaultBizLoginService] username or password is empty, login failed!");
            return Optional.empty();
        }

        final Optional<UserInfoDO> userInfoOpt = userInfoRepository.findByUsername(username);
        if (!userInfoOpt.isPresent()) {
            log.debug("[DefaultBizLoginService] can't find user by username: {}", username);
            return Optional.empty();
        }

        final UserInfoDO dbUser = userInfoOpt.get();

        if (s(username, password).equals(dbUser.getPassword())) {
            BizUser bizUser = new BizUser();
            bizUser.setUsername(username);
            return Optional.of(bizUser);
        }

        log.debug("[DefaultBizLoginService] user[{}]'s password is not correct, login failed!", username);
        return Optional.empty();
    }

    private static String s(String username, String password) {
        String f1 = String.format("%s_%s_z", username, password);
        return String.format("%s_%s_b", username, DigestUtils.md5(f1));
    }
}
