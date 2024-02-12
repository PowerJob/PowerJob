package tech.powerjob.server.auth.common;

/**
 * 常量
 *
 * @author tjq
 * @since 2024/2/11
 */
public class AuthConstants {

    /**
     * JWT key
     * 前端 header 默认首字母大写，保持一致方便处理
     */
    public static final String JWT_NAME = "Power_jwt";

    /**
     * 前端跳转到指定页面指令
     */
    public static final String FE_REDIRECT_KEY = "FE-REDIRECT:";

    public static final String TIPS_NO_PERMISSION_TO_SEE = "NO_PERMISSION_TO_SEE";
}
