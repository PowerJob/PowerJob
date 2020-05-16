package com.github.kfcfans.oms.worker.common;

/**
 * 异常
 *
 * @author tjq
 * @since 2020/5/16
 */
public class OmsWorkerException extends RuntimeException {

    public OmsWorkerException() {
    }

    public OmsWorkerException(String message) {
        super(message);
    }

    public OmsWorkerException(String message, Throwable cause) {
        super(message, cause);
    }

    public OmsWorkerException(Throwable cause) {
        super(cause);
    }

    public OmsWorkerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
