package com.github.kfcfans.oms.client;

/**
 * 异常
 *
 * @author tjq
 * @since 2020/5/14
 */
public class OmsOpenApiException extends RuntimeException {

    public OmsOpenApiException() {
    }

    public OmsOpenApiException(String message) {
        super(message);
    }

    public OmsOpenApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public OmsOpenApiException(Throwable cause) {
        super(cause);
    }

    public OmsOpenApiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
