package tech.powerjob.worker.pojo.request;

import tech.powerjob.common.PowerSerializable;
import tech.powerjob.worker.persistence.TaskDO;
import tech.powerjob.worker.pojo.model.InstanceInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * TaskTracker 派发 task 进行执行
 *
 * @author tjq
 * @since 2020/3/17
 */
@Getter
@Setter
@NoArgsConstructor
public class TaskTrackerStartTaskReq implements PowerSerializable {

    // TaskTracker 地址
    private String taskTrackerAddress;
    private InstanceInfo instanceInfo;

    private String taskId;
    private String taskName;
    private byte[] taskContent;
    // 子任务当前重试次数
    private int taskCurrentRetryNums;

    // 秒级任务专用
    private long subInstanceId;

    private String logConfig;

    private String advancedRuntimeConfig;

    /**
     * 创建 TaskTrackerStartTaskReq，该构造方法必须在 TaskTracker 节点调用
     */
    public TaskTrackerStartTaskReq(InstanceInfo instanceInfo, TaskDO task, String taskTrackerAddress) {

        this.taskTrackerAddress = taskTrackerAddress;
        this.instanceInfo = instanceInfo;

        this.taskId = task.getTaskId();
        this.taskName = task.getTaskName();
        this.taskContent = task.getTaskContent();

        this.taskCurrentRetryNums = task.getFailedCnt();
        this.subInstanceId = task.getSubInstanceId();

        this.logConfig = instanceInfo.getLogConfig();
        this.advancedRuntimeConfig = instanceInfo.getAdvancedRuntimeConfig();
    }
}
