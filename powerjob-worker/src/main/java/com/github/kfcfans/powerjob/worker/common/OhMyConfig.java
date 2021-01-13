package com.github.kfcfans.powerjob.worker.common;

import com.github.kfcfans.powerjob.common.RemoteConstant;
import com.github.kfcfans.powerjob.worker.common.constants.StoreStrategy;
import com.github.kfcfans.powerjob.worker.core.processor.ProcessResult;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * The powerjob-worker's configuration
 *
 * @author tjq
 * @since 2020/3/16
 */
@Getter
@Setter
public class OhMyConfig {
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
     * If test mode is set as true, Powerjob-worker no longer connects to the server or validates appName.
     * Test mode is used for conditions that your have no powerjob-server in your develop env so you can't startup the application
     */
    private boolean enableTestMode = false;
}
