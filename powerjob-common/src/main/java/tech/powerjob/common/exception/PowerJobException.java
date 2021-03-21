package tech.powerjob.common.exception;

/**
 * PowerJob 运行时异常
 *
 * @author tjq
 * @since 2020/5/26
 */
public class PowerJobException extends RuntimeException {

    public PowerJobException() {
    }

    public PowerJobException(String message) {
        super(message);
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
