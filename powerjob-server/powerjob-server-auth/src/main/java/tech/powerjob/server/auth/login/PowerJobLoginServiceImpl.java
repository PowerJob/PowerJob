package tech.powerjob.server.auth.login;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tech.powerjob.server.auth.PowerJobUser;
import tech.powerjob.server.auth.login.biz.LoginService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * PowerJobLoginService
 *
 * @author tjq
 * @since 2023/3/21
 */
@Service
public class PowerJobLoginServiceImpl implements PowerJobLoginService {

    private final Map<String, LoginService> type2LoginService = Maps.newHashMap();

    private static final String TOKEN_HEADER_NAME = "power_token";

    @Autowired
    public PowerJobLoginServiceImpl(List<LoginService> loginServices) {
        loginServices.forEach(k -> type2LoginService.put(k.type(), k));
    }


    @Override
    public Optional<PowerJobUser> login(LoginContext loginContext) {
        final String loginType = loginContext.getLoginType();
        final LoginService loginService = type2LoginService.get(loginType);
        if (loginService == null) {
            throw new IllegalArgumentException("can't find LoginService by type: " + loginType);
        }
        return loginService.login(loginContext);
    }

    @Override
    public Optional<PowerJobUser> parse(LoginContext loginContext) {
        final HttpServletRequest httpServletRequest = loginContext.getHttpServletRequest();
        final String tokenHeader = httpServletRequest.getHeader(TOKEN_HEADER_NAME);
        if (StringUtils.isEmpty(tokenHeader)) {
            return Optional.empty();
        }
        return Optional.empty();
    }
}
