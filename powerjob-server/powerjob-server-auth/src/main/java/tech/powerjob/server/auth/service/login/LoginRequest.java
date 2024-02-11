package tech.powerjob.server.auth.service.login;

import lombok.Data;

import javax.servlet.http.HttpServletRequest;

/**
 * 执行登录的请求
 *
 * @author tjq
 * @since 2024/2/10
 */
@Data
public class LoginRequest {

    /**
     * 登录类型
     */
    private String loginType;

    /**
     * 原始参数，给第三方登录方式一个服务端和前端交互的数据通道。PowerJob 本身不感知其中的内容
     */
    private String originParams;

    private transient HttpServletRequest httpServletRequest;
}
