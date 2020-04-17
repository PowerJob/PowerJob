package com.github.kfcfans.oms.worker.common;

import com.github.kfcfans.oms.worker.common.constants.StoreStrategy;
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
     * 本地持久化方式，默认使用磁盘
     */
    private StoreStrategy storeStrategy = StoreStrategy.DISK;
    /**
     * 最大返回值长度，超过会被截断
     */
    private int maxResultLength = 8096;

}
