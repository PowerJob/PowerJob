package com.github.kfcfans.powerjob.worker.common.utils;

import com.github.kfcfans.powerjob.common.utils.CommonUtils;
import com.github.kfcfans.powerjob.worker.OhMyWorker;

/**
 * 文件工具类
 *
 * @author tjq
 * @since 2020/5/16
 */
public class OmsWorkerFileUtils {

    private static final String USER_HOME = System.getProperty("user.home", "powerjob");
    private static final String WORKER_DIR = USER_HOME + "/powerjob/" + OhMyWorker.getConfig().getAppName() + "/";

    public static String getScriptDir() {
        return WORKER_DIR + "script/";
    }

    public static String getContainerDir() {
        return WORKER_DIR + "container/";
    }

    public static String getH2WorkDir() {
        return WORKER_DIR + "h2/" + CommonUtils.genUUID() + "/";
    }
}
