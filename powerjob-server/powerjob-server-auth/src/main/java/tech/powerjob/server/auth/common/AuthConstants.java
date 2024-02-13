package tech.powerjob.server.auth.common;

/**
 * 常量
 *
 * @author tjq
 * @since 2024/2/11
 */
public class AuthConstants {

    /* ********** 账号体系唯一标识，推荐开发者接入第三方登录体系时也使用4位编码，便于前端统一做样式 ********** */
    /**
     * PowerJob自建账号体系
     */
    public static final String ACCOUNT_TYPE_POWER_JOB = "PWJB";
    /**
     * 钉钉
     */
    public static final String ACCOUNT_TYPE_DING = "DING";
    /**
     * 企业微信（预留，蹲一个 contributor）
     */
    public static final String ACCOUNT_TYPE_WX = "QYWX";
    /**
     * 飞书（预留，蹲一个 contributor +1）
     */
    public static final String ACCOUNT_LARK = "LARK";

    /* ********** 账号体系 ********** */

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
