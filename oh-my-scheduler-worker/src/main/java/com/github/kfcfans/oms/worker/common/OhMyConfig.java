package com.github.kfcfans.oms.worker.common;

import com.github.kfcfans.oms.common.RemoteConstant;
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
     * 启动端口
     */
    private int port = RemoteConstant.DEFAULT_WORKER_PORT;
    /**
     * 调度服务器地址，ip:port 或 域名
     */
    private List<String> serverAddress;
    /**
     * 本地持久化方式，默认使用磁盘
     */
    private StoreStrategy storeStrategy = StoreStrategy.DISK;
    /**
     * 最大返回值长度，超过会被截断
     * {@link com.github.kfcfans.oms.worker.core.processor.ProcessResult}#msg 的最大长度
     */
    private int maxResultLength = 8096;

    /**
     * 启动测试模式，true情况下，不再尝试连接 server 并验证appName
     * true -> 用于本地写单元测试调试； false -> 默认值，标准模式
     */
    private boolean enableTestMode = false;
}
