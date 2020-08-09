package com.github.kfcfans.powerjob.worker.autoconfigure;

import com.github.kfcfans.powerjob.common.RemoteConstant;
import com.github.kfcfans.powerjob.worker.common.constants.StoreStrategy;
import com.github.kfcfans.powerjob.worker.core.processor.ProcessResult;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;

/**
 * PowerJob 配置项
 *
 * @author songyinyin
 * @since 2020/7/26 16:37
 */
@ConfigurationProperties(prefix = "powerjob")
public class PowerJobProperties {

    private final Worker worker = new Worker();

    public Worker getWorker() {
        return worker;
    }

    @Deprecated
    @DeprecatedConfigurationProperty(replacement = "powerjob.worker.app-name")
    public String getAppName() {
        return getWorker().appName;
    }

    @Deprecated
    public void setAppName(String appName) {
        getWorker().setAppName(appName);
    }

    @Deprecated
    @DeprecatedConfigurationProperty(replacement = "powerjob.worker.akka-port")
    public int getAkkaPort() {
        return getWorker().akkaPort;
    }

    @Deprecated
    public void setAkkaPort(int akkaPort) {
        getWorker().setAkkaPort(akkaPort);
    }

    @Deprecated
    @DeprecatedConfigurationProperty(replacement = "powerjob.worker.server-address")
    public String getServerAddress() {
        return getWorker().serverAddress;
    }

    @Deprecated
    public void setServerAddress(String serverAddress) {
        getWorker().setServerAddress(serverAddress);
    }

    @Deprecated
    @DeprecatedConfigurationProperty(replacement = "powerjob.worker.store-strategy")
    public StoreStrategy getStoreStrategy() {
        return getWorker().storeStrategy;
    }

    @Deprecated
    public void setStoreStrategy(StoreStrategy storeStrategy) {
        getWorker().setStoreStrategy(storeStrategy);
    }

    @Deprecated
    @DeprecatedConfigurationProperty(replacement = "powerjob.worker.max-result-length")
    public int getMaxResultLength() {
        return getWorker().maxResultLength;
    }

    @Deprecated
    public void setMaxResultLength(int maxResultLength) {
        getWorker().setMaxResultLength(maxResultLength);
    }

    @Deprecated
    @DeprecatedConfigurationProperty(replacement = "powerjob.worker.enable-test-mode")
    public boolean isEnableTestMode() {
        return getWorker().enableTestMode;
    }

    @Deprecated
    public void setEnableTestMode(boolean enableTestMode) {
        getWorker().setEnableTestMode(enableTestMode);
    }



    /**
     * 客户端 配置项
     */
    @Setter
    @Getter
    public static class Worker {
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
        private String serverAddress;
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
}
