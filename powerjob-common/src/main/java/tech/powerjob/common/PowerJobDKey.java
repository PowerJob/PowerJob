package tech.powerjob.common;

import java.net.NetworkInterface;

/**
 * 通过 JVM 启动参数传入的配置信息
 *
 *
 * @author tjq
 * @since 2020/8/8
 */
public class PowerJobDKey {

    /**
     * The property name for {@link NetworkInterface#getDisplayName() the name of network interface} that the PowerJob application prefers
     */
    public static final String PREFERRED_NETWORK_INTERFACE = "powerjob.network.interface.preferred";

    public static final String BIND_LOCAL_ADDRESS = "powerjob.network.local.address";

    /**
     * Java regular expressions for network interfaces that will be ignored.
     */
    public static final String IGNORED_NETWORK_INTERFACE_REGEX = "powerjob.network.interface.ignored";

    public static final String WORKER_STATUS_CHECK_PERIOD = "powerjob.worker.status-check.normal.period";

    /**
     * allowed PowerJob to invoke Thread#stop to kill a thread when PowerJob can't interrupt the thread
     * <a href="https://stackoverflow.com/questions/16504140/thread-stop-deprecated">It's VERY dangerous</a>
     */
    public static final String WORKER_ALLOWED_FORCE_STOP_THREAD = "powerjob.worker.allowed-force-stop-thread";
    /**
     * ms
     */
    public static final String FREQUENCY_JOB_MAX_INTERVAL = "powerjob.server.frequency-job.max-interval";

}
