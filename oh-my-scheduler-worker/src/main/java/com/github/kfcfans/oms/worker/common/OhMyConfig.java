package com.github.kfcfans.oms.worker.common;

import lombok.Data;

/**
 * Worker 配置文件
 *
 * @author tjq
 * @since 2020/3/16
 */
@Data
public class OhMyConfig {
    /**
     * 应用名称
     */
    private String appName;
    /**
     * 调度服务器地址，ip:port （多值使用 , 分隔）
     */
    private String serverAddress;
    /**
     * 通讯端口
     */
    private int listeningPort;
}
