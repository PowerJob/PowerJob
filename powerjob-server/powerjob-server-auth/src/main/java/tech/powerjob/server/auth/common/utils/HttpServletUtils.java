package tech.powerjob.server.auth.common.utils;

import tech.powerjob.common.OmsConstant;

import javax.servlet.http.HttpServletRequest;

/**
 * HttpServletUtils
 *
 * @author tjq
 * @since 2024/2/12
 */
public class HttpServletUtils {

    public static String fetchFromHeader(String key, HttpServletRequest httpServletRequest) {
        // header、cookie 都能获取
        String v = httpServletRequest.getHeader(key);

        // 解决 window.localStorage.getItem 为 null 的问题
        if (OmsConstant.NULL.equalsIgnoreCase(v) || "undefined".equalsIgnoreCase(v)) {
            return null;
        }

        return v;
    }

}
