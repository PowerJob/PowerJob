package com.github.kfcfans.oms.worker.common;

import lombok.Data;

import java.util.List;

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
     * 调度服务器地址，ip:port
     */
    private List<String> serverAddress;
    /**
     * 通讯端口
     */
    private String listeningIP;
    private Integer listeningPort;
}
