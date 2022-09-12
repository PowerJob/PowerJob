package tech.powerjob.server.monitor.events.db;

/**
 * DatabaseEventType
 *
 * @author tjq
 * @since 2022/9/6
 */
public enum DatabaseType {
    /**
     * 本地存储库，H2
     */
    LOCAL,
    /**
     * 远程核心库
     */
    CORE,
    /**
     * 扩展库
     */
    EXTRA
}
