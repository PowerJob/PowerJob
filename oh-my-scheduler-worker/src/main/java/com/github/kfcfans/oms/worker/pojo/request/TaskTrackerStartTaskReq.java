package com.github.kfcfans.oms.worker.pojo.request;

import com.github.kfcfans.oms.worker.OhMyWorker;
import com.github.kfcfans.oms.worker.persistence.TaskDO;
import com.github.kfcfans.oms.worker.pojo.model.InstanceInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * TaskTracker 派发 task 进行执行
 *
 * @author tjq
 * @since 2020/3/17
 */
@Getter
@Setter
@NoArgsConstructor
public class TaskTrackerStartTaskReq implements Serializable {

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


    /**
     * 创建 TaskTrackerStartTaskReq，该构造方法必须在 TaskTracker 节点调用
     */
    public TaskTrackerStartTaskReq(InstanceInfo instanceInfo, TaskDO task) {

        this.taskTrackerAddress = OhMyWorker.getWorkerAddress();
        this.instanceInfo = instanceInfo;

        this.taskId = task.getTaskId();
        this.taskName = task.getTaskName();
        this.taskContent = task.getTaskContent();

        this.taskCurrentRetryNums = task.getFailedCnt();
        this.subInstanceId = task.getSubInstanceId();
    }
}
