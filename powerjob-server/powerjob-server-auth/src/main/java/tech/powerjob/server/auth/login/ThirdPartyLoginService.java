package tech.powerjob.server.auth.login;

/**
 * 第三方登录服务
 *
 * @author tjq
 * @since 2024/2/10
 */
public interface ThirdPartyLoginService {

    /**
     * 登陆服务的类型
     * @return 登陆服务类型，比如 PowerJob / DingTalk
     */
    LoginTypeInfo loginType();

    /**
     * 生成登陆的重定向 URL
     * @param loginContext 上下文
     * @return 重定向地址
     */
    String generateLoginUrl(LoginContext loginContext);

    /**
     * 执行第三方登录
     * @param loginContext 上下文
     * @return 登录地址
     */
    ThirdPartyUser login(LoginContext loginContext);

}
