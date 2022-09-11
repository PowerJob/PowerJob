package tech.powerjob.server.monitor.events.w2s;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import tech.powerjob.server.common.SJ;
import tech.powerjob.server.monitor.Event;

/**
 * description
 *
 * @author tjq
 * @since 2022/9/11
 */
@Setter
@Accessors(chain = true)
public class WorkerLogReportEvent implements Event {

    private String workerAddress;

    private long logSize;

    private Status status;

    private long serverCost;

    public enum Status {
        SUCCESS,
        REJECTED,
        EXCEPTION
    }

    @Override
    public String type() {
        return "MONITOR_LOGGER_WORKER_LOG_REPORT";
    }

    @Override
    public String message() {
        return SJ.MONITOR_JOINER.join(workerAddress, logSize, status, serverCost);
    }
}
