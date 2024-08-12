package tech.powerjob.common.utils;

import org.apache.commons.lang3.StringUtils;
import tech.powerjob.common.PowerJobDKey;

/**
 * Debug 工具
 *
 * @author tjq
 * @since 2024/8/13
 */
public class DebugUtils {

    private static Boolean debugMode;

    /**
     * 判断当前是否处在 PowerJob 的 Debug 模式
     * @return inDebugMode
     */
    public static boolean inDebugMode() {

        if (debugMode != null) {
            return debugMode;
        }

        String debug = PropertyUtils.readProperty(PowerJobDKey.DEBUG_LEVEL, null);
        debugMode = StringUtils.isNotEmpty(debug);
        return debugMode;
    }
}
