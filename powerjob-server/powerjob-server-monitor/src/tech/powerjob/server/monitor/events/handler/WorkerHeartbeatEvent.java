package tech.powerjob.server.monitor.events.handler;

import lombok.Setter;
import lombok.experimental.Accessors;
import tech.powerjob.server.common.SJ;
import tech.powerjob.server.monitor.Event;

/**
 * worker 心跳事件监控
 *
 * @author tjq
 * @since 2022/9/9
 */
@Setter
@Accessors(chain = true)
public class WorkerHeartbeatEvent implements Event {

    private String appName;

    private String version;

    private String protocol;

    private String tag;
    private String workerAddress;

    private Integer score;

    @Override
    public String type() {
        return "MONITOR_LOGGER_WORKER_HEART_BEAT";
    }

    @Override
    public String message() {
        return SJ.MONITOR_JOINER.join(appName, version, protocol, tag, workerAddress, score);
    }
}
