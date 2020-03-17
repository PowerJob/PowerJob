package com.github.kfcfans.oms.worker.common.utils;

import org.springframework.context.ApplicationContext;

/**
 * Spring ApplicationContext 工具类
 *
 * @author tjq
 * @since 2020/3/16
 */
public class SpringUtils {

    private static boolean supportSpringBean = false;
    private static ApplicationContext context;

    public static void inject(ApplicationContext ctx) {
        context = ctx;
        supportSpringBean = true;
    }

    public static boolean supportSpringBean() {
        return supportSpringBean;
    }

    public static <T> T getBean(Class<T> clz) {
        return context.getBean(clz);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getBean(String className) {
        return (T) context.getBean(className);
    }

}
