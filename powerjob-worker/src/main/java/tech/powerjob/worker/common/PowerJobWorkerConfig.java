package tech.powerjob.worker.common;

import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Setter;
import tech.powerjob.common.RemoteConstant;
import tech.powerjob.common.enums.Protocol;
import tech.powerjob.worker.common.constants.StoreStrategy;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.WorkflowContext;
import tech.powerjob.worker.extension.SystemMetricsCollector;
import tech.powerjob.worker.extension.processor.ProcessorFactory;

import java.util.List;

/**
 * The powerjob-worker's configuration
 *
 * @author tjq
 * @since 2020/3/16
 */
@Getter
@Setter
public class PowerJobWorkerConfig {
    /**
     * AppName, recommend to use the name of this project
     * Applications should be registered by powerjob-console in advance to prevent error.
     */
    private String appName;
    /**
     * Worker port
     * Random port is enabled when port is set with non-positive number.
     */
    private int port = RemoteConstant.DEFAULT_WORKER_PORT;
    /**
     * Address of powerjob-server node(s)
     * Do not mistake for ActorSystem port. Do not add any prefix, i.e. http://.
     */
    private List<String> serverAddress = Lists.newArrayList();
    /**
     * Protocol for communication between WORKER and server
     */
    private Protocol protocol = Protocol.AKKA;
    /**
     * Max length of response result. Result that is longer than the value will be truncated.
     * {@link ProcessResult} max length for #msg
     */
    private int maxResultLength = 8096;
    /**
     * User-defined context object, which is passed through to the TaskContext#userContext property
     * Usage Scenarios: The container Java processor needs to use the Spring bean of the host application, where you can pass in the ApplicationContext and get the bean in the Processor
     */
    private Object userContext;
    /**
     * Internal persistence method, DISK or MEMORY
     * Normally you don't need to care about this configuration
     */
    private StoreStrategy storeStrategy = StoreStrategy.DISK;
    /**
     * If allowLazyConnectServer is set as true, PowerJob worker allows launching without a direct connection to the server.
     * allowLazyConnectServer is used for conditions that your have no powerjob-server in your develop env so you can't startup the application
     */
    private boolean allowLazyConnectServer = false;
    /**
     * Max length of appended workflow context value length. Appended workflow context value that is longer than the value will be ignore.
     * {@link WorkflowContext} max length for #appendedContextData
     */
    private int maxAppendedWfContextLength = 8192;
    /**
     * user-customized system metrics collector
     */
    private SystemMetricsCollector systemMetricsCollector;
    /**
     * Processor factory for custom logic, generally used for IOC framework processor bean injection that is not officially supported by PowerJob
     */
    private List<ProcessorFactory> processorFactoryList;

    private String tag;
    /**
     * Max numbers of LightTaskTacker
     */
    private Integer maxLightweightTaskNum = 1024;
    /**
     * Max numbers of HeavyTaskTacker
     */
    private Integer maxHeavyweightTaskNum = 64;
    /**
     * Interval(s) of worker health report
     */
    private Integer healthReportInterval = 10;

}
