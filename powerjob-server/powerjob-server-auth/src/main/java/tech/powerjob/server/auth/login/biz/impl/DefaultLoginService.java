package tech.powerjob.server.auth.login.biz.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tech.powerjob.server.auth.PowerJobUser;
import tech.powerjob.server.auth.login.LoginContext;
import tech.powerjob.server.auth.login.biz.LoginService;
import tech.powerjob.server.persistence.remote.repository.UserInfoRepository;

import javax.annotation.Resource;
import java.util.Optional;

/**
 * PowerJob 自带的登陆服务
 *
 * @author tjq
 * @since 2023/3/20
 */
@Slf4j
@Service
public class DefaultLoginService implements LoginService {

    @Resource
    private UserInfoRepository userInfoRepository;

    public static final String DEFAULT_LOGIN_SERVICE = "PowerJob";

    @Override
    public String type() {
        return DEFAULT_LOGIN_SERVICE;
    }

    @Override
    public Optional<PowerJobUser> login(LoginContext loginContext) {
        return Optional.empty();
    }
}
