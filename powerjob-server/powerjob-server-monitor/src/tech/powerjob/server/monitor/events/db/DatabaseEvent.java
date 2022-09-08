package tech.powerjob.server.monitor.events.db;

import lombok.Setter;
import lombok.experimental.Accessors;
import tech.powerjob.server.common.SJ;
import tech.powerjob.server.monitor.Event;

/**
 * 数据库操作事件
 *
 * @author tjq
 * @since 2022/9/6
 */
@Setter
@Accessors(chain = true)
public class DatabaseEvent implements Event {

    private DatabaseType type;

    private String serviceName;

    private String methodName;

    private Status status;

    private Integer rows;

    private long cost;

    private String errorMsg;

    private String extra;

    public enum Status {
        SUCCESS,
        FAILED
    }

    @Override
    public String type() {
        return "MONITOR_LOGGER_DB_OPERATION";
    }

    @Override
    public String message() {
        return SJ.MONITOR_JOINER.join(type, serviceName, methodName, status, rows, cost, errorMsg, extra);
    }
}
