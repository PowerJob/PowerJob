package tech.powerjob.server.monitor;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

    private final List<Monitor> monitors = Lists.newLinkedList();

    public PowerJobMonitorService(List<Monitor> monitors) {
        monitors.forEach(m -> {
            log.info("[MonitorService] register monitor: {}", m.getClass().getName());
            this.monitors.add(m);
        });
    }

    @Override
    public void monitor(Event event) {
        monitors.forEach(m -> m.record(event));
    }
}
