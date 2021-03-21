package tech.powerjob.official.processors.util;

import org.apache.commons.lang3.StringUtils;

/**
 * Some dangerous processors must be passed in the specified JVM startup parameters to be enabled
 *
 * @author tjq
 * @since 2021/3/14
 */
public class SecurityUtils {

    public static final String ENABLE_FILE_CLEANUP_PROCESSOR = "powerjob.official-processor.file-cleanup.enable";

    public static final String ENABLE_DYNAMIC_SQL_PROCESSOR = "powerjob.official-processor.dynamic-datasource.enable";

    public static boolean disable(String dKey) {
        if (StringUtils.isEmpty(dKey)) {
            return false;
        }
        String property = System.getProperty(dKey);
        return !StringUtils.equals(Boolean.TRUE.toString(), property);
    }
}
