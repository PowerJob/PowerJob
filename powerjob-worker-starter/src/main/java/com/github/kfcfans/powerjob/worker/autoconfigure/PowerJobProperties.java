package com.github.kfcfans.powerjob.worker.autoconfigure;

import com.github.kfcfans.powerjob.common.RemoteConstant;
import com.github.kfcfans.powerjob.worker.common.constants.StoreStrategy;
import com.github.kfcfans.powerjob.worker.core.processor.ProcessResult;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * PowerJob 配置项
 *
 * @author songyinyin
 * @date 2020/7/26 16:37
 */
@Data
@ConfigurationProperties(prefix = "powerjob")
public class PowerJobProperties {
    /**
     * 应用名称，需要提前在控制台注册，否则启动报错
     */
    private String appName;
    /**
     * 启动 akka 端口
     */
    private int akkaPort = RemoteConstant.DEFAULT_WORKER_PORT;
    /**
     * 调度服务器地址，ip:port 或 域名，多个用英文逗号分隔
     */
    private String serverAddress = "127.0.0.1:7700";
    /**
     * 本地持久化方式，默认使用磁盘
     */
    private StoreStrategy storeStrategy = StoreStrategy.DISK;
    /**
     * 最大返回值长度，超过会被截断
     * {@link ProcessResult}#msg 的最大长度
     */
    private int maxResultLength = 8096;
    /**
     * 启动测试模式，true情况下，不再尝试连接 server 并验证appName。
     * true -> 用于本地写单元测试调试； false -> 默认值，标准模式
     */
    private boolean enableTestMode = false;
}
