package tech.powerjob.worker.common.constants;

/**
 * task 常量
 *
 * @author tjq
 * @since 2020/3/17
 */
public class TaskConstant {

    private TaskConstant() {

    }

    /**
     * 所有根任务的名称
     */
    public static final String ROOT_TASK_NAME = "OMS_ROOT_TASK";
    /**
     * 广播执行任务的名称
     */
    public static final String BROADCAST_TASK_NAME = "OMS_BROADCAST_TASK";
    /**
     * 终极任务的名称（MapReduce的reduceTask和Broadcast的postProcess会有该任务）
     */
    public static final String LAST_TASK_NAME = "OMS_LAST_TASK";

}
