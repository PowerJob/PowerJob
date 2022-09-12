package tech.powerjob.server.common.constants;

/**
 * 线程池
 *
 * @author tjq
 * @since 2022/9/12
 */
public class PJThreadPool {

    /**
     * 定时调度用线程池
     */
    public static final String TIMING_POOL = "PowerJobTimingPool";

    /**
     * 后台任务异步线程池
     */
    public static final String BACKGROUND_POOL = "PowerJobBackgroundPool";

    /**
     * 本地数据库专用线程池
     */
    public static final String LOCAL_DB_POOL = "PowerJobLocalDbPool";

}
