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

    public static final String WORKER_ACTOR_SYSTEM_NAME = "oms";

    public static final String TASK_TRACKER_ACTOR_NAME = "task_tracker";
    public static final String PROCESSOR_TRACKER_ACTOR_NAME = "processor_tracker";
    public static final String WORKER_ACTOR_NAME = "worker";
    public static final String TROUBLESHOOTING_ACTOR_NAME = "troubleshooting";

    public static final String WORKER_AKKA_CONFIG_NAME = "oms-worker.akka.conf";


    /* ************************ AKKA SERVER ************************ */
    public static final String SERVER_ACTOR_SYSTEM_NAME = "oms-server";

    public static final String SERVER_ACTOR_NAME = "server_actor";
    public static final String SERVER_FRIEND_ACTOR_NAME = "friend_actor";
    public static final String SERVER_AKKA_CONFIG_NAME = "oms-server.akka.conf";


    /* ************************ OTHERS ************************ */
    public static final String EMPTY_ADDRESS = "N/A";
    public static final long DEFAULT_TIMEOUT_MS = 5000;

    /* ************************ SERVER ************************ */
    public static final String SERVER_PATH = "server";
    /**
     * server 处理在线日志
     */
    public static final String SERVER_HANDLER_REPORT_LOG = "reportLog";
    /**
     * server 处理 worker 心跳
     */
    public static final String SERVER_HANDLER_WORKER_HEARTBEAT = "workerHeartbeat";

    /**
     * server 处理 TaskTracker 上报的任务实例状态
     */
    public static final String SERVER_HANDLER_REPORT_INSTANCE_STATUS = "reportInstanceStatus";

    /**
     * server 查询任务的可执行集群
     */
    public static final String SERVER_HANDLER_QUERY_JOB_CLUSTER = "queryJobCluster";

    /**
     * server 处理 worker 请求部署容器命令
     */
    public static final String SERVER_HANDLER_WORKER_NEED_DEPLOY_CONTAINER = "container";

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
