package tech.powerjob.server.web.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求
 *
 * @author tjq
 * @since 2023/3/26
 */
@Data
public class UserLoginRequest implements Serializable {

    private String type;

    private String loginInfo;
}
