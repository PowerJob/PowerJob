package tech.powerjob.server.monitor.monitors.impl;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tech.powerjob.server.monitor.Event;
import tech.powerjob.server.monitor.Monitor;

/**
 * 系统默认实现——基于日志的监控监视器
 * 需要接入方自行基于类 ELK 系统采集
 *
 * @author tjq
 * @since 2022/9/6
 */
@Component
public class LogMonitor implements Monitor {

    @Override
    public void record(Event event) {
        LoggerFactory.getLogger(event.type()).info(event.message());
    }
}
