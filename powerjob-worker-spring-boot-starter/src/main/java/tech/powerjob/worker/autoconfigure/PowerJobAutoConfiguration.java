package tech.powerjob.worker.autoconfigure;

import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.common.utils.NetUtils;
import tech.powerjob.worker.PowerJobWorker;
import tech.powerjob.worker.common.PowerJobWorkerConfig;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Auto configuration class for PowerJob-worker.
 *
 * @author songyinyin
 * @since 2020/7/26 16:37
 */
@Configuration
@EnableConfigurationProperties(PowerJobProperties.class)
@Conditional(PowerJobAutoConfiguration.PowerJobWorkerCondition.class)
public class PowerJobAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PowerJobWorker initPowerJob(PowerJobProperties properties) {

        PowerJobProperties.Worker worker = properties.getWorker();

        /*
         * Address of PowerJob-server node(s). Do not mistake for ActorSystem port. Do not add
         * any prefix, i.e. http://.
         */
        CommonUtils.requireNonNull(worker.getServerAddress(), "serverAddress can't be empty!");
        List<String> serverAddress = Arrays.asList(worker.getServerAddress().split(","));

        /*
         * Create OhMyConfig object for setting properties.
         */
        PowerJobWorkerConfig config = new PowerJobWorkerConfig();
        /*
         * Configuration of worker port. Random port is enabled when port is set with non-positive number.
         */
        int port = worker.getAkkaPort();
        if (port <= 0) {
            port = NetUtils.getRandomPort();
        }
        config.setPort(port);
        /*
         * appName, name of the application. Applications should be registered in advance to prevent
         * error. This property should be the same with what you entered for appName when getting
         * registered.
         */
        config.setAppName(worker.getAppName());
        config.setServerAddress(serverAddress);
        /*
         * For non-Map/MapReduce tasks, {@code memory} is recommended for speeding up calculation.
         * Map/MapReduce tasks may produce batches of subtasks, which could lead to OutOfMemory
         * exception or error, {@code disk} should be applied.
         */
        config.setStoreStrategy(worker.getStoreStrategy());
        /*
         * When enabledTestMode is set as true, PowerJob-worker no longer connects to PowerJob-server
         * or validate appName.
         */
        config.setEnableTestMode(worker.isEnableTestMode());
        /*
         * Max length of appended workflow context . Appended workflow context value that is longer than the value will be ignore.
         */
        config.setMaxAppendedWfContextLength(worker.getMaxAppendedWfContextLength());
        /*
         * Worker Tag
         */
        config.setTag(worker.getTag());
        /*
         * Create OhMyWorker object and set properties.
         */
        PowerJobWorker ohMyWorker = new PowerJobWorker();
        ohMyWorker.setConfig(config);
        return ohMyWorker;
    }

    static class PowerJobWorkerCondition extends AnyNestedCondition {

        public PowerJobWorkerCondition() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @Deprecated
        @ConditionalOnProperty(prefix = "powerjob", name = "server-address")
        static class PowerJobProperty {

        }

        @ConditionalOnProperty(prefix = "powerjob.worker", name = "server-address")
        static class PowerJobWorkerProperty {

        }
    }
}
