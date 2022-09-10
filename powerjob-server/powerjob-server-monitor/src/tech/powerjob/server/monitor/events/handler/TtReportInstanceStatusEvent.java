package tech.powerjob.server.monitor.events.handler;

import lombok.Setter;
import lombok.experimental.Accessors;
import tech.powerjob.server.monitor.Event;

/**
 * TaskTrackerReportInstanceStatus
 *
 * @author tjq
 * @since 2022/9/9
 */
@Setter
@Accessors(chain = true)
public class TtReportInstanceStatusEvent implements Event {

    @Override
    public String type() {
        return "MONITOR_LOGGER_TT_REPORT_STATUS";
    }

    @Override
    public String message() {
        return null;
    }
}
