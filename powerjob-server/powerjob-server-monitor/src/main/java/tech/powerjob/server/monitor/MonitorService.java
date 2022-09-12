package tech.powerjob.server.monitor;

/**
 * 对外暴露的监控服务
 *
 * @author tjq
 * @since 2022/9/10
 */
public interface MonitorService {
    void monitor(Event event);
}
