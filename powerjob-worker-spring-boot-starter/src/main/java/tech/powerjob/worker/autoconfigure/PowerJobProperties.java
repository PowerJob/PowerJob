package tech.powerjob.worker.autoconfigure;

import tech.powerjob.common.RemoteConstant;
import tech.powerjob.worker.common.constants.StoreStrategy;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.WorkflowContext;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;

/**
 * PowerJob properties configuration class.
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

    @Deprecated
    @DeprecatedConfigurationProperty(replacement = "powerjob.worker.tag")
    public String getTag() {
        return getWorker().tag;
    }

    @Deprecated
    public void setTag(String tag) {
        getWorker().setTag(tag);
    }

    /**
     * Powerjob worker configuration properties.
     */
    @Setter
    @Getter
    public static class Worker {
        /**
         * Name of application, String type. Total length of this property should be no more than 255
         * characters. This is one of the required properties when registering a new application. This
         * property should be assigned with the same value as what you entered for the appName.
         */
        private String appName;
        /**
         * Akka port of Powerjob-worker, optional value. Default value of this property is 27777.
         * If multiple PowerJob-worker nodes were deployed, different, unique ports should be assigned.
         */
        private int akkaPort = RemoteConstant.DEFAULT_WORKER_PORT;
        /**
         * Address(es) of Powerjob-server node(s). Ip:port or domain.
         * Example of single Powerjob-server node:
         * <p>
         * 127.0.0.1:7700
         * </p>
         * Example of Powerjob-server cluster:
         * <p>
         * 192.168.0.10:7700,192.168.0.11:7700,192.168.0.12:7700
         * </p>
         */
        private String serverAddress;
        /**
         * Local store strategy for H2 database. {@code disk} or {@code memory}.
         */
        private StoreStrategy storeStrategy = StoreStrategy.DISK;
        /**
         * Max length of response result. Result that is longer than the value will be truncated.
         * {@link ProcessResult} max length for #msg
         */
        private int maxResultLength = 8192;
        /**
         * If test mode is set as true, Powerjob-worker no longer connects to the server or validates appName.
         * Test mode is used for conditions that your have no powerjob-server in your develop env so you can't startup the application
         */
        private boolean enableTestMode = false;
        /**
         * Max length of appended workflow context value length. Appended workflow context value that is longer than the value will be ignore.
         * {@link WorkflowContext} max length for #appendedContextData
         */
        private int maxAppendedWfContextLength = 8192;
        /**
         * Worker Tag
         */
        private String tag;
    }
}
