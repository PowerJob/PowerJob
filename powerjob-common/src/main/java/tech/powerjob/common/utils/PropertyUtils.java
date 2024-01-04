package tech.powerjob.common.utils;

import org.apache.commons.lang3.StringUtils;

/**
 * PropertyUtils
 *
 * @author tjq
 * @since 2023/7/15
 */
public class PropertyUtils {

    public static String readProperty(String key, String defaultValue) {
        // 从启动参数读取
        String property = System.getProperty(key);
        if (StringUtils.isNotEmpty(property)) {
            return property;
        }

        // 从 ENV 读取
        property= System.getenv(key);
        if (StringUtils.isNotEmpty(property)) {
            return property;
        }
        // 部分操作系统不兼容 a.b.c 的环境变量，转换为 a_b_c 再取一次，即 PowerJob 支持 2 种类型的环境变量 key
        property = System.getenv(key.replaceAll("\\.", "_"));
        if (StringUtils.isNotEmpty(property)) {
            return property;
        }
        return defaultValue;
    }
}
