package com.github.kfcfans.powerjob.worker.common.utils;

import com.github.kfcfans.powerjob.common.utils.CommonUtils;
import com.github.kfcfans.powerjob.worker.OhMyWorker;
import com.github.kfcfans.powerjob.worker.common.OhMyConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * 文件工具类
 *
 * @author tjq
 * @since 2020/5/16
 */
@Slf4j
public class OmsWorkerFileUtils {

    private static String basePath;

    public static void init(OhMyConfig config) {
        String userHome = System.getProperty("user.home", "powerjob");
        basePath = userHome + "/powerjob/" + config.getAppName() + "/";
        log.info("[PowerFile] use base file path: {}", basePath);
    }

    public static String getScriptDir() {
        return basePath + "script/";
    }

    public static String getContainerDir() {
        return basePath + "container/";
    }

    public static String getH2WorkDir() {
        return basePath + "h2/" + CommonUtils.genUUID() + "/";
    }
}
