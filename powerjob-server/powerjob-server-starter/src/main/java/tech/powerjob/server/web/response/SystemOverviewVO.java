package tech.powerjob.server.web.response;

import lombok.Data;
import tech.powerjob.server.common.module.ServerInfo;

/**
 * 系统概览
 *
 * @author tjq
 * @since 2020/4/14
 */
@Data
public class SystemOverviewVO {

    private Long appId;

    private String appName;

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

    /**
     * 处理当前 WEB 服务的 server 信息
     */
    private ServerInfo webServerInfo;
    /**
     * 调度服务器信息
     */
    private ServerInfo scheduleServerInfo;
}
