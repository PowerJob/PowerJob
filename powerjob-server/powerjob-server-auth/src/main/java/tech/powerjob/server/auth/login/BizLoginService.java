package tech.powerjob.server.auth.login;

import tech.powerjob.server.auth.LoginContext;

import java.util.Optional;

/**
 * 用户登陆服务
 *
 * @author tjq
 * @since 2023/3/20
 */
public interface BizLoginService {

    /**
     * 登陆服务的类型
     * @return 登陆服务类型，比如 PowerJob / DingTalk
     */
    String type();

    /**
     * 登陆的重定向 URL
     * @return 重定向地址
     */
    String loginUrl();

    /**
     * 执行登陆
     * @param loginContext 登陆上下文
     * @return PowerJob 用户
     */
    Optional<BizUser> login(LoginContext loginContext);
}
