package tech.powerjob.server.auth.service.login;

import tech.powerjob.server.auth.PowerJobUser;
import tech.powerjob.server.auth.common.PowerJobAuthException;
import tech.powerjob.server.auth.login.LoginTypeInfo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

/**
 * PowerJob 登录服务
 *
 * @author tjq
 * @since 2024/2/10
 */
public interface PowerJobLoginService {

    /**
     * 获取全部可登录的类型
     * @return 全部可登录类型
     */
    List<LoginTypeInfo> fetchSupportLoginTypes();


    /**
     * 获取第三方登录链接
     * @param loginType 登录类型
     * @param httpServletRequest http请求
     * @return 重定向地址
     */
    String fetchThirdPartyLoginUrl(String loginType, HttpServletRequest httpServletRequest);

    /**
     * 执行真正的登录请求，底层调用第三方登录服务完成登录
     * @param loginRequest 登录请求
     * @return 登录完成的 PowerJobUser
     * @throws PowerJobAuthException 鉴权失败抛出异常
     */
    PowerJobUser doLogin(LoginRequest loginRequest) throws PowerJobAuthException;

    /**
     * 从 JWT 信息中解析用户登录信息
     * @param httpServletRequest httpServletRequest
     * @return PowerJob 用户
     */
    Optional<PowerJobUser> ifLogin(HttpServletRequest httpServletRequest);
}
