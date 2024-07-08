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

    public static final String PARAM_KEY_USERNAME = "username";
    public static final String PARAM_KEY_PASSWORD = "password";
    /**
     * 前端参数-密码加密类型，官方版本出于成本未进行前后端传输的对称加密，接入方有需求可自行实现，此处定义加密协议字段
     */
    public static final String PARAM_KEY_ENCRYPTION = "encryption";

    /* ********** 账号体系 ********** */

    /**
     * JWT key
     * 前端 header 默认首字母大写，保持一致方便处理
     */
    public static final String OLD_JWT_NAME = "Power_jwt";
    public static final String JWT_NAME = "PowerJwt";

    /**
     * 前端跳转到指定页面指令
     */
    public static final String FE_REDIRECT_KEY = "FE-REDIRECT:";

    public static final String TIPS_NO_PERMISSION_TO_SEE = "NO_PERMISSION_TO_SEE";

    public static final Long GLOBAL_ADMIN_TARGET_ID = 1L;
}
