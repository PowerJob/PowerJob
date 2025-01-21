package tech.powerjob.server.auth.common.utils;

import javax.servlet.http.HttpServletRequest;

/**
 * 获取常用的请求头
 *
 * @author tjq
 * @since 2025/1/21
 */
public class HttpHeaderUtils {

    public static String fetchAppId(HttpServletRequest request) {
        return HttpServletUtils.fetchFromHeader("AppId", request);
    }

    public static String fetchNamespaceId(HttpServletRequest request) {
        return HttpServletUtils.fetchFromHeader("NamespaceId", request);
    }
}
