package tech.powerjob.server.auth.common;

import lombok.Getter;

/**
 * 鉴权相关错误
 *
 * @author tjq
 * @since 2024/2/10
 */
@Getter
public class PowerJobAuthException extends RuntimeException {

    private final String code;

    private final String msg;

    public PowerJobAuthException(AuthErrorCode errorCode) {
        this.code = errorCode.getCode();
        this.msg = errorCode.getMsg();
    }

    public PowerJobAuthException(AuthErrorCode errorCode, String extraMsg) {
        this.code = errorCode.getCode();
        this.msg = errorCode.getMsg().concat(":").concat(extraMsg);
    }
}
