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
}
