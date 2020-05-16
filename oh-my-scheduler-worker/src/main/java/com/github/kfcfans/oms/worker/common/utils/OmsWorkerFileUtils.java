package com.github.kfcfans.oms.worker.common.utils;

/**
 * 文件工具类
 *
 * @author tjq
 * @since 2020/5/16
 */
public class OmsWorkerFileUtils {

    private static final String USER_HOME = System.getProperty("user.home", "oms");
    private static final String WORKER_DIR = USER_HOME + "/oms/";

    public static String getScriptDir() {
        return WORKER_DIR + "script/";
    }

    public static String getContainerDir() {
        return WORKER_DIR + "container/";
    }
}
