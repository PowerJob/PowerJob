package tech.powerjob.server.auth.common;

import lombok.Getter;
import tech.powerjob.common.exception.PowerJobException;

/**
 * 鉴权相关错误
 *
 * @author tjq
 * @since 2024/2/10
 */
@Getter
public class PowerJobAuthException extends PowerJobException {

    public PowerJobAuthException(AuthErrorCode errorCode) {
        this(errorCode, null);
    }

    public PowerJobAuthException(AuthErrorCode errorCode, String extraMsg) {
        super(extraMsg == null ? errorCode.getMsg() : errorCode.getMsg().concat(":").concat(extraMsg));
        this.code = errorCode.getCode();
    }
}
