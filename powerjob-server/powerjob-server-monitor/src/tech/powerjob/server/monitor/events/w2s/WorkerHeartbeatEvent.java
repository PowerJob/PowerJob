package tech.powerjob.server.monitor.events.w2s;

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
    /**
     * 虽然和 AppName 冗余，但考虑到其他日志使用 appId 监控，此处可方便潜在的其他处理
     */
    private Long appId;
    private String version;

    private String protocol;

    private String tag;
    private String workerAddress;
    /**
     * worker 上报时间与 server 之间的延迟
     */
    private long delayMs;
    private Integer score;

    @Override
    public String type() {
        return "MONITOR_LOGGER_WORKER_HEART_BEAT";
    }

    @Override
    public String message() {
        return SJ.MONITOR_JOINER.join(appName, appId, version, protocol, tag, workerAddress, delayMs, score);
    }
}
