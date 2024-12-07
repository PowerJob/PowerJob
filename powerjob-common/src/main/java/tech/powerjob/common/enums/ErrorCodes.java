package tech.powerjob.common.enums;

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
public enum ErrorCodes {

    USER_NOT_LOGIN("-100", "UserNotLoggedIn"),
    USER_NOT_EXIST("-101", "UserNotExist"),
    USER_AUTH_FAILED("-102", "UserAuthFailed"),
    /**
     * 账户被停用
     */
    USER_DISABLED("-103", "UserDisabled"),


    NO_PERMISSION("-200", "NoPermission"),

    /**
     * 无效请求，一般是参数问题
     */
    INVALID_REQUEST("-300", "INVALID_REQUEST"),

    INCORRECT_PASSWORD("-400", "INCORRECT_PASSWORD"),

    /**
     * 非法令牌
     */
    INVALID_TOKEN("-401", "INVALID_TOKEN"),
    /**
     * 无效 APP（无法找到 app）
     */
    INVALID_APP("-402", "INVALID_APP"),

    /**
     * 令牌过期
     */
    TOKEN_EXPIRED("-403", "TOKEN_EXPIRED"),

    /**
     * 系统内部异常
     */
    SYSTEM_UNKNOWN_ERROR("-500", "SYS_UNKNOWN_ERROR"),
    /**
     * 非法参数
     */
    ILLEGAL_ARGS_ERROR("-501", "ILLEGAL_ARGS_ERROR"),

    /**
     * OPENAPI 错误码号段 -10XX
     */
    OPEN_API_AUTH_FAILED("-1002", "OPEN_API_AUTH_FAILED"),

    /**
     * PowerJobClient 错误码号段
     */
    CLIENT_HTTP_REQUEST_FAILED("-2001", "CLIENT_HTTP_REQUEST_FAILED"),

    ;

    private final String code;
    private final String msg;
}
