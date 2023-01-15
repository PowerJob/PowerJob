package tech.powerjob.server.web.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import tech.powerjob.server.common.module.ServerInfo;

/**
 * 系统概览
 *
 * @author tjq
 * @since 2020/4/14
 */
@Data
public class SystemOverviewVO {

    private long jobCount;
    private long runningInstanceCount;
    private long failedInstanceCount;
    /**
     * 服务器时区
     */
    private String timezone;
    /**
     * 服务器时间
     */
    private String serverTime;

    private ServerInfo serverInfo;
}
