package tech.powerjob.server.monitor;

/**
 * 监控事件
 *
 * @author tjq
 * @since 2022/9/6
 */
public interface Event {

    /**
     * 监控事件的类型
     * @return 监控类型
     */
    String type();

    /**
     * 监控事件的内容
     * @return 监控事件的内容
     */
    String message();
}
