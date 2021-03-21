package tech.powerjob.common.exception;

/**
 * PowerJob 受检异常，需要开发者手动处理
 *
 * @author KFC·D·Fans
 * @since 2021/3/21
 */
public class PowerJobCheckedException extends Exception {

    public PowerJobCheckedException() {
    }

    public PowerJobCheckedException(String message) {
        super(message);
    }

    public PowerJobCheckedException(String message, Throwable cause) {
        super(message, cause);
    }

    public PowerJobCheckedException(Throwable cause) {
        super(cause);
    }

    public PowerJobCheckedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
