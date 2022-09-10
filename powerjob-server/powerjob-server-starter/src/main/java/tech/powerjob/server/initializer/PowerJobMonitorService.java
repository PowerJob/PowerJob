package tech.powerjob.server.initializer;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.powerjob.server.monitor.Event;
import tech.powerjob.server.monitor.Monitor;
import tech.powerjob.server.monitor.MonitorContext;
import tech.powerjob.server.monitor.MonitorService;
import tech.powerjob.server.remote.server.ServerInfoService;

import javax.annotation.Resource;
import java.util.List;

/**
 * PowerJob 服务端监控
 *
 * @author tjq
 * @since 2022/9/10
 */
@Slf4j
@Component
public class PowerJobMonitorService implements MonitorService {

    @Resource
    private ServerInfoService service;

    private final List<Monitor> monitors = Lists.newLinkedList();

    @Autowired
    public PowerJobMonitorService(List<Monitor> monitors) {

        MonitorContext monitorContext = new MonitorContext().setServerId(service.getServerId()).setServerAddress(service.getServerIp());
        log.info("[MonitorService] use monitor context: {}", monitorContext);

        monitors.forEach(m -> {
            log.info("[MonitorService] register monitor: {}", m.getClass().getName());
            m.init(monitorContext);
            this.monitors.add(m);
        });
    }

    @Override
    public void monitor(Event event) {
        monitors.forEach(m -> m.record(event));
    }
}
