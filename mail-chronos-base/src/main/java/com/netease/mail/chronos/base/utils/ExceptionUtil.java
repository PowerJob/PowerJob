package com.netease.mail.chronos.base.utils;

import org.apache.commons.lang.exception.ExceptionUtils;

/**
 * @author Echo009
 * @since 2021/9/18
 */
public final class ExceptionUtil {

    private ExceptionUtil() {

    }


    public static String getExceptionDesc(Throwable throwable){

        Throwable rootCause = ExceptionUtils.getRootCause(throwable);

        if (rootCause == null){
            rootCause = throwable;
        }

        // only obtain first line
        String fullStackTrace = ExceptionUtils.getFullStackTrace(rootCause);
        return fullStackTrace.split("\n")[0];

    }

}
