package tech.powerjob.server.auth.login.biz;

import tech.powerjob.server.auth.PowerJobUser;
import tech.powerjob.server.auth.login.LoginContext;

import java.util.Optional;

/**
 * 用户登陆服务
 *
 * @author tjq
 * @since 2023/3/20
 */
public interface LoginService {

    /**
     * 登陆服务的类型
     * @return 登陆服务类型，比如 PowerJob / DingTalk
     */
    String type();

    /**
     * 执行登陆
     * @param loginContext 登陆上下文
     * @return PowerJob 用户
     */
    Optional<PowerJobUser> login(LoginContext loginContext);
}
