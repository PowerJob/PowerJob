package com.github.kfcfans.powerjob.common;

/**
 * 系统生成的任务实例运行结果
 *
 * @author tjq
 * @since 2020/4/11
 */
public class SystemInstanceResult {

    /* *********** 普通instance 专用 *********** */

    // 同时运行的任务实例数过多
    public static final String TOO_MANY_INSTANCES = "too many instances(%d>%d)";
    // 无可用worker
    public static final String NO_WORKER_AVAILABLE = "no worker available";
    // 任务执行超时
    public static final String INSTANCE_EXECUTE_TIMEOUT = "instance execute timeout";
    // 创建根任务失败
    public static final String TASK_INIT_FAILED = "create root task failed";
    // 未知错误
    public static final String UNKNOWN_BUG = "unknown bug";
    // TaskTracker 长时间未上报
    public static final String REPORT_TIMEOUT = "worker report timeout, maybe TaskTracker down";

    /* *********** workflow 专用 *********** */
    public static final String MIDDLE_JOB_FAILED = "middle job failed";
    public static final String MIDDLE_JOB_STOPPED = "middle job stopped by user";
    public static final String CAN_NOT_FIND_JOB = "can't find some job";

    // 被用户手动停止
    public static final String STOPPED_BY_USER = "stopped by user";
    public static final String CANCELED_BY_USER = "canceled by user";


}
