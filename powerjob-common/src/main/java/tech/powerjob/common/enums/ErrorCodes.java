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

    INVALID_TOKEN("-401", "INVALID_TOKEN"),

    INVALID_APP("-402", "INVALID_APP"),

    /**
     * 系统内部异常
     */
    SYSTEM_UNKNOWN_ERROR("-500", "SYS_UNKNOWN_ERROR"),

    /**
     * OPENAPI 错误码号段 -10XX
     */
    OPEN_API_PASSWORD_ERROR("-1001", "OPEN_API_PASSWORD_ERROR"),
    OPEN_API_AUTH_FAILED("-1002", "OPEN_API_AUTH_FAILED"),

    ;

    private final String code;
    private final String msg;
}
