package com.github.kfcfans.powerjob.common;

/**
 * OhMyScheduler 运行时异常
 *
 * @author tjq
 * @since 2020/5/26
 */
public class OmsException extends RuntimeException {

    public OmsException() {
    }

    public OmsException(String message) {
        super(message);
    }

    public OmsException(String message, Throwable cause) {
        super(message, cause);
    }

    public OmsException(Throwable cause) {
        super(cause);
    }

    public OmsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
