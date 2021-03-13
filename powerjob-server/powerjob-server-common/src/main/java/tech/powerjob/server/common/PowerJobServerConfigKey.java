package tech.powerjob.server.common;

/**
 * 配置文件 key
 *
 * @author tjq
 * @since 2020/8/2
 */
public class PowerJobServerConfigKey {

    /**
     * akka 协议端口号
     */
    public static final String AKKA_PORT = "oms.akka.port";
    /**
     * http 协议端口号
     */
    public static final String HTTP_PORT = "oms.http.port";
    /**
     * 自定义数据库表前缀
     */
    public static final String TABLE_PREFIX = "oms.table-prefix";
    /**
     * 是否使用 mongoDB
     */
    public static final String MONGODB_ENABLE = "oms.mongodb.enable";
    /**
     * 是否启用 Swagger-UI，默认关闭
     */
    public static final String SWAGGER_UI_ENABLE = "oms.swagger.enable";

    /**
     * 钉钉报警相关
     */
    public static final String DING_APP_KEY = "oms.alarm.ding.app-key";
    public static final String DING_APP_SECRET = "oms.alarm.ding.app-secret";
    public static final String DING_AGENT_ID = "oms.alarm.ding.agent-id";
}
