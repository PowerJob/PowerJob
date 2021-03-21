package tech.powerjob.server.common.utils;

import tech.powerjob.common.utils.CommonUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;

/**
 * 文件工具类，统一文件存放地址
 *
 * @author tjq
 * @since 2020/5/15
 */
public class OmsFileUtils {

    private static final String USER_HOME = System.getProperty("user.home", "oms");
    private static final String COMMON_PATH = USER_HOME + "/powerjob/server/";

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
        return COMMON_PATH + "container/";
    }

    /**
     * 获取临时目录（固定目录）
     * @return 目录
     */
    public static String genTemporaryPath() {
        return COMMON_PATH + "temporary/";
    }

    /**
     * 获取临时目录（随机目录，不会重复），用完记得删除
     * @return 临时目录
     */
    public static String genTemporaryWorkPath() {
        return genTemporaryPath() + CommonUtils.genUUID() + "/";
    }

    /**
     * 获取 H2 数据库工作目录
     * @return H2 工作目录
     */
    public static String genH2BasePath() {
        return COMMON_PATH + "h2/";
    }
    public static String genH2WorkPath() {
        return genH2BasePath() + CommonUtils.genUUID() + "/";
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

    /**
     * 输出文件（对外下载功能）
     * @param file 文件
     * @param response HTTP响应
     * @throws IOException 异常
     */
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

    /**
     * 计算文件的 MD5
     * @param f 文件
     * @return md5
     * @throws IOException 异常
     */
    public static String md5(File f) throws IOException {
        String md5;
        try(FileInputStream fis = new FileInputStream(f)) {
            md5 = DigestUtils.md5DigestAsHex(fis);
        }
        return md5;
    }
}
