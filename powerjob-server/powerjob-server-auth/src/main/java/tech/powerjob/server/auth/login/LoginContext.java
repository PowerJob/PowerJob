package tech.powerjob.server.auth.login;

import lombok.Data;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录上下文
 *
 * @author tjq
 * @since 2024/2/10
 */
@Data
public class LoginContext {

    /**
     * 原始参数，给第三方登录方式一个服务端和前端交互的数据通道。PowerJob 本身不感知其中的内容
     */
    private String originParams;

    private transient HttpServletRequest httpServletRequest;

    private transient HttpServletResponse httpServletResponse;
}
