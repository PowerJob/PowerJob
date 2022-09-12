package tech.powerjob.server.monitor.events.w2s;

import lombok.Setter;
import lombok.experimental.Accessors;
import tech.powerjob.common.enums.InstanceStatus;
import tech.powerjob.server.common.SJ;
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

    private Long appId;
    private Long jobId;
    private Long instanceId;

    private Long wfInstanceId;

    private InstanceStatus instanceStatus;

    private Long delayMs;

    private Status serverProcessStatus;

    private Long serverProcessCost;

    public enum Status {
        SUCCESS,
        FAILED
    }

    @Override
    public String type() {
        return "MONITOR_LOGGER_TT_REPORT_STATUS";
    }

    @Override
    public String message() {
        return SJ.MONITOR_JOINER.join(appId, jobId, instanceId, wfInstanceId, instanceStatus, delayMs, serverProcessStatus, serverProcessCost);
    }
}
