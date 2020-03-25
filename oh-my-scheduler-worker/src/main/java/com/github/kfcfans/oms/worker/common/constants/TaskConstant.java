package com.github.kfcfans.oms.worker.common.constants;

/**
 * task 常熟
 *
 * @author tjq
 * @since 2020/3/17
 */
public class TaskConstant {

    /**
     * 所有根任务的名称
     */
    public static final String ROOT_TASK_NAME = "OMS_ROOT_TASK";
    /**
     * 所有根任务的ID
     */
    public static final String ROOT_TASK_ID = "0";

    /**
     * 广播执行任务的名称
     */
    public static final String BROADCAST_TASK_NAME = "OMS_BROADCAST_TASK";
    /**
     * 终极任务的名称（MapReduce的reduceTask和Broadcast的postProcess会有该任务）
     */
    public static final String LAST_TASK_NAME = "OMS_LAST_TASK";
    // 除0外任何数都可以
    public static final String LAST_TASK_ID = "9999";

}
