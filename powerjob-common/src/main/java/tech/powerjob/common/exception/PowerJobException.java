package tech.powerjob.common.exception;

import lombok.Getter;
import lombok.Setter;
import tech.powerjob.common.enums.ErrorCodes;

/**
 * PowerJob 运行时异常
 *
 * @author tjq
 * @since 2020/5/26
 */
@Setter
@Getter
public class PowerJobException extends RuntimeException {

    protected String code;

    public PowerJobException() {
    }

    public PowerJobException(String message) {
        super(message);
    }

    public PowerJobException(ErrorCodes errorCode, String extraMsg) {
        super(extraMsg == null ? errorCode.getMsg() : errorCode.getMsg().concat(":").concat(extraMsg));
        this.code = errorCode.getCode();
    }

    public PowerJobException(String message, Throwable cause) {
        super(message, cause);
    }

    public PowerJobException(Throwable cause) {
        super(cause);
    }

    public PowerJobException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
