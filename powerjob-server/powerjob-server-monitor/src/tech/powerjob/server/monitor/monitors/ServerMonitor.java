package tech.powerjob.server.monitor.monitors;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.powerjob.server.monitor.Event;
import tech.powerjob.server.monitor.Monitor;

import java.util.List;

/**
 * 服务端监视器
 *
 * @author tjq
 * @since 2022/9/6
 */
@Slf4j
@Component
public class ServerMonitor implements Monitor {

    private final List<Monitor> monitors = Lists.newLinkedList();

    @Autowired
    public ServerMonitor(List<Monitor> monitors) {
        monitors.forEach(m -> {
            if (m == this) {
                return;
            }
            log.info("[ServerMonitor] register monitor: {}", m.getClass().getName());
            this.monitors.add(m);
        });
    }

    @Override
    public void record(Event event) {
        monitors.forEach(m -> m.record(event));
    }
}
