package com.github.kfcfans.oms.server.common.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.UUID;

/**
 * 文件工具类，统一文件存放地址
 *
 * @author tjq
 * @since 2020/5/15
 */
public class OmsFileUtils {

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
     * 获取临时目录，用完记得删除
     * @return 临时目录
     */
    public static String genTemporaryPath() {
        String uuid = StringUtils.replace(UUID.randomUUID().toString(), "-", "");
        return COMMON_PATH + "temporary/" + uuid + "/";
    }

    /**
     * 为目标文件创建父文件夹
     * @param file 目标文件
     */
    public static void forceMkdir4Parent(File file) {
        File directory = file.getParentFile();
        forceMkdir(directory);
    }

    public static void forceMkdir(File directory) {
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

    /**
     * 将文本写入文件
     * @param content 文本内容
     * @param file 文件
     */
    public static void string2File(String content, File file) {
        try(FileWriter fw = new FileWriter(file)) {
            fw.write(content);
        }catch (IOException ie) {
            ExceptionUtils.rethrow(ie);
        }
    }

    public static void file2HttpResponse(File file, HttpServletResponse response) throws IOException {

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(file.getName(), "UTF-8"));

        byte[] buffer = new byte[4096];
        try (BufferedOutputStream bos = new BufferedOutputStream(response.getOutputStream());
             BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {

            while (bis.read(buffer) != -1) {
                bos.write(buffer);
            }
        }
    }
}
