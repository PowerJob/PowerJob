package tech.powerjob.server.monitor;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 日志全局上下文
 *
 * @author tjq
 * @since 2022/9/10
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
public class MonitorContext implements Serializable {
    private long serverId;
    private String serverAddress;

}
