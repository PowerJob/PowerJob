package tech.powerjob.common;

import java.net.NetworkInterface;

/**
 * 通过 JVM 启动参数传入的配置信息
 *
 * @author tjq
 * @since 2020/8/8
 */
public class PowerJobDKey {

    /**
     * The property name for {@link NetworkInterface#getDisplayName() the name of network interface} that the PowerJob application prefers
     */
    public static final String PREFERRED_NETWORK_INTERFACE = "powerjob.network.interface.preferred";

    /**
     * 绑定地址，一般填写本机网卡地址
     */
    public static final String BIND_LOCAL_ADDRESS = "powerjob.network.local.address";
    /**
     * 外部地址，可选，默认与绑定地址相同。当存在 NAT 等场景时可通过单独传递外部地址来实现通讯
     */
    public static final String NT_EXTERNAL_ADDRESS = "powerjob.network.external.address";
    public static final String NT_EXTERNAL_PORT = "powerjob.network.external.port";

    /**
     * Java regular expressions for network interfaces that will be ignored.
     */
    public static final String IGNORED_NETWORK_INTERFACE_REGEX = "powerjob.network.interface.ignored";

    /**
     * Enables compression during data transfer, such as gzip under the HTTP protocol. default value is 'false'
     * Note that enabling compression reduces network usage, but increases CPU consumption
     */
    public static final String TRANSPORTER_USE_COMPRESSING = "powerjob.transporter.compression.enabled";

    /**
     * keep-alive connection timeout(in seconds), value <= 0 means disable keepalive. default value is 75
     */
    public static final String TRANSPORTER_KEEP_ALIVE_TIMEOUT = "powerjob.transporter.keepalive.timeout";

    public static final String WORKER_STATUS_CHECK_PERIOD = "powerjob.worker.status-check.normal.period";

    /**
     * allowed PowerJob to invoke Thread#stop to kill a thread when PowerJob can't interrupt the thread
     * <a href="https://stackoverflow.com/questions/16504140/thread-stop-deprecated">It's VERY dangerous</a>
     */
    public static final String WORKER_ALLOWED_FORCE_STOP_THREAD = "powerjob.worker.allowed-force-stop-thread";

    public static final String WORKER_WORK_SPACE = "powerjob.worker.workspace";
    /**
     * ms
     */
    public static final String FREQUENCY_JOB_MAX_INTERVAL = "powerjob.server.frequency-job.max-interval";

}
