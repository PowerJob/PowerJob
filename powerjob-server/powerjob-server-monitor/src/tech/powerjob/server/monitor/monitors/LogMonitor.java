package tech.powerjob.server.monitor.monitors;

import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import tech.powerjob.server.monitor.Event;
import tech.powerjob.server.monitor.Monitor;
import tech.powerjob.server.monitor.MonitorContext;

/**
 * 系统默认实现——基于日志的监控监视器
 * 需要接入方自行基于类 ELK 系统采集
 *
 * @author tjq
 * @since 2022/9/6
 */
@Component
public class LogMonitor implements Monitor {

    private MonitorContext monitorContext;

    private static final String MDC_KEY_SERVER_ID = "serverId";
    private static final String MDC_KEY_SERVER_ADDRESS = "serverAddress";

    @Override
    public void init(MonitorContext monitorContext) {
        this.monitorContext = monitorContext;
    }

    @Override
    public void record(Event event) {
        MDC.put(MDC_KEY_SERVER_ID, String.valueOf(monitorContext.getServerId()));
        MDC.put(MDC_KEY_SERVER_ADDRESS, monitorContext.getServerAddress());
        LoggerFactory.getLogger(event.type()).info(event.message());
    }
}
