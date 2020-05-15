package com.github.kfcfans.oms.server.common.utils;

import java.io.File;
import java.io.IOException;

/**
 * 文件工具类，统一文件存放地址
 *
 * @author tjq
 * @since 2020/5/15
 */
public class OmsFilePathUtils {

    private static final String USER_HOME = System.getProperty("user.home", "oms");
    private static final String COMMON_PATH = USER_HOME + "/oms-server/";

    /**
     * 获取在线日志的存放路径
     * @return 在线日志的存放路径
     */
    public static String genLogDirPath() {
        return COMMON_PATH + "online_log/";
    }

    /**
     * 获取用于构建容器的 jar 文件存放路径
     * @return 路径
     */
    public static String genContainerJarPath() {
        return COMMON_PATH + "container/jar/";
    }

    /**
     * 为目标文件创建父文件夹
     * @param file 目标文件
     */
    public static void forceMkdir(File file) {
        File directory = file.getParentFile();
        if (directory.exists()) {
            if (!directory.isDirectory()) {
                final String message =
                        "File "
                                + directory
                                + " exists and is "
                                + "not a directory. Unable to create directory.";
                throw new RuntimeException(message);
            }
        } else {
            if (!directory.mkdirs()) {
                // Double-check that some other thread or process hasn't made
                // the directory in the background
                if (!directory.isDirectory()) {
                    final String message =
                            "Unable to create directory " + directory;
                    throw new RuntimeException(message);
                }
            }
        }
    }
}
