package tech.powerjob.server.auth.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 鉴权错误信息
 *
 * @author tjq
 * @since 2024/2/11
 */
@Getter
@AllArgsConstructor
public enum AuthErrorCode {

    USER_NOT_LOGIN("-100", "UserNotLoggedIn"),
    USER_NOT_EXIST("-101", "UserNotExist"),


    NO_PERMISSION("-200", "NoPermission"),

    /**
     * 无效请求，一般是参数问题
     */
    INVALID_REQUEST("-300", "INVALID_REQUEST")

    ;

    private final String code;
    private final String msg;
}
