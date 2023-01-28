package tech.powerjob.common;

/**
 * RemoteConstant
 *
 * @author tjq
 * @since 2020/3/17
 */
public class RemoteConstant {


    /* ************************ AKKA WORKER ************************ */
    public static final int DEFAULT_WORKER_PORT = 27777;


    /* ************************ OTHERS ************************ */
    public static final String EMPTY_ADDRESS = "N/A";
    public static final long DEFAULT_TIMEOUT_MS = 5000;

    /* ************************ SERVER-self_side (s4s == server for server side) ************************ */
    public static final String S4S_PATH = "friend";

    /**
     * server 集群间的心跳处理
     */
    public static final String S4S_HANDLER_PING = "ping";
    /**
     * 处理其他 server 的执行请求
     */
    public static final String S4S_HANDLER_PROCESS = "process";

    /* ************************ SERVER-worker_side（s4w == server for worker side） ************************ */
    public static final String S4W_PATH = "server";
    /**
     * server 处理在线日志
     */
    public static final String S4W_HANDLER_REPORT_LOG = "reportLog";
    /**
     * server 处理 worker 心跳
     */
    public static final String S4W_HANDLER_WORKER_HEARTBEAT = "workerHeartbeat";

    /**
     * server 处理 TaskTracker 上报的任务实例状态
     */
    public static final String S4W_HANDLER_REPORT_INSTANCE_STATUS = "reportInstanceStatus";

    /**
     * server 查询任务的可执行集群
     */
    public static final String S4W_HANDLER_QUERY_JOB_CLUSTER = "queryJobCluster";

    /**
     * server 处理 worker 请求部署容器命令
     */
    public static final String S4W_HANDLER_WORKER_NEED_DEPLOY_CONTAINER = "queryContainer";

    /* ************************ Worker-TaskTracker ************************ */
    public static final String WTT_PATH = "taskTracker";

    /**
     * server 任务执行命令
     */
    public static final String WTT_HANDLER_RUN_JOB = "runJob";
    /**
     * server 停止任务实例命令
     */
    public static final String WTT_HANDLER_STOP_INSTANCE = "stopInstance";

    /**
     * sever 查询任务状态
     */
    public static final String WTT_HANDLER_QUERY_INSTANCE_STATUS = "queryInstanceStatus";

    /**
     * PT 上报任务状态，包含执行结果
     */
    public static final String WTT_HANDLER_REPORT_TASK_STATUS = "reportTaskStatus";
    /**
     * PT 上报自身状态
     */
    public static final String WTT_HANDLER_REPORT_PROCESSOR_TRACKER_STATUS = "reportProcessorTrackerStatus";

    /**
     * Map 任务
     */
    public static final String WTT_HANDLER_MAP_TASK = "mapTask";

    /* ************************ Worker-ProcessorTracker ************************ */
    public static final String WPT_PATH = "processorTracker";

    public static final String WPT_HANDLER_START_TASK = "startTask";

    public static final String WPT_HANDLER_STOP_INSTANCE = "stopInstance";

    /* ************************ Worker-NORMAL ************************ */

    public static final String WORKER_PATH = "worker";

    public static final String WORKER_HANDLER_DEPLOY_CONTAINER = "deployContainer";

    public static final String WORKER_HANDLER_DESTROY_CONTAINER = "destroyContainer";

}
