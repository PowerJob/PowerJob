package tech.powerjob.server.monitor.events.lock;

import lombok.Setter;
import lombok.experimental.Accessors;
import tech.powerjob.server.common.SJ;
import tech.powerjob.server.monitor.Event;

/**
 * 长时间等待锁事件
 *
 * @author tjq
 * @since 2022/9/9
 */
@Setter
@Accessors(chain = true)
public class SlowLockEvent implements Event {

    private String locKType;
    private String lockKey;
    private String callerService;
    private String callerMethod;
    private long cost;

    @Override
    public String type() {
        return "MONITOR_LOGGER_SLOW_LOCK";
    }

    @Override
    public String message() {
        return SJ.MONITOR_JOINER.join(locKType, lockKey, callerService, callerMethod, cost);
    }
}
