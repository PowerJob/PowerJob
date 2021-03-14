package tech.powerjob.common;

/**
 * HttpProtocolConstant
 *
 * @author tjq
 * @since 2021/2/8
 */
public class ProtocolConstant {

    public static final String SERVER_PATH_HEARTBEAT = "/server/heartbeat";
    public static final String SERVER_PATH_STATUS_REPORT = "/server/statusReport";
    public static final String SERVER_PATH_LOG_REPORT = "/server/logReport";

    public static final String WORKER_PATH_DISPATCH_JOB = "/worker/runJob";
    public static final String WORKER_PATH_STOP_INSTANCE = "/worker/stopInstance";
    public static final String WORKER_PATH_QUERY_INSTANCE_INFO = "/worker/queryInstanceInfo";
}
