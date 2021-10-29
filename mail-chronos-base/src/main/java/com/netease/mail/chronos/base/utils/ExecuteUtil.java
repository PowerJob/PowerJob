package com.netease.mail.chronos.base.utils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Echo009
 * @since 2021/10/29
 */
@Slf4j
public class ExecuteUtil<T> {

    private ExecuteUtil(){

    }

    @SuppressWarnings("all")
    public static void executeIgnoreExceptionWithoutReturn(Action action,String logMark){
        try {
            action.exec();
        }catch (Throwable e){
            log.error("{},execute failed!",logMark,e);
        }
    }

    @SuppressWarnings("all")
    public static void executeIgnoreExceptionWithoutReturn(Action action){
        try {
            action.exec();
        }catch (Throwable ignore){
            // ignore
        }
    }

    @SuppressWarnings("all")
    public static <T> void executeIgnoreSpecifiedExceptionWithoutReturn(Action action,Class<T> clazz){
        try {
            action.exec();
        }catch (Throwable e){
            if (!e.getClass().equals(clazz)) {
                throw e;
            }
        }
    }

    @FunctionalInterface
    public interface Action{

        void exec();

    }
}
