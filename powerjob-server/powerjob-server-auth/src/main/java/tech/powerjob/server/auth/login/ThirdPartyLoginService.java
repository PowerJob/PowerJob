package tech.powerjob.server.auth.login;

import javax.servlet.http.HttpServletRequest;

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
     * @param httpServletRequest http请求
     * @return 重定向地址
     */
    String generateLoginUrl(HttpServletRequest httpServletRequest);

    /**
     * 执行第三方登录
     * @param loginRequest 上下文
     * @return 登录地址
     */
    ThirdPartyUser login(ThirdPartyLoginRequest loginRequest);

    /**
     * JWT 登录的回调校验
     * @param username 用户名称
     * @param tokenLoginVerifyInfo 二次校验信息
     * @return 是否通过
     */
    default boolean tokenLoginVerify(String username, TokenLoginVerifyInfo tokenLoginVerifyInfo) {
        return true;
    }
}
