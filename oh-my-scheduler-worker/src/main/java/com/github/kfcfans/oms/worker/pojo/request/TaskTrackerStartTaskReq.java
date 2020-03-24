package com.github.kfcfans.oms.worker.pojo.request;

import com.github.kfcfans.oms.worker.common.utils.NetUtils;
import com.github.kfcfans.oms.worker.persistence.TaskDO;
import com.github.kfcfans.oms.worker.pojo.model.JobInstanceInfo;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * JobTracker 派发 task 进行执行
 *
 * @author tjq
 * @since 2020/3/17
 */
@Getter
@Setter
public class TaskTrackerStartTaskReq implements Serializable {

    private String jobId;
    private String instanceId;
    // 任务执行类型，单机、广播、MR
    private String executeType;
    // 处理器类型（JavaBean、Jar、脚本等）
    private String processorType;
    // 处理器信息
    private String processorInfo;
    // 并发计算线程数
    private int threadConcurrency;
    // TaskTracker 地址
    private String taskTrackerAddress;
    // 任务超时时间
    private long jobTimeLimitMS;

    private String jobParams;
    private String instanceParams;

    private String taskId;
    private String taskName;
    private byte[] subTaskContent;
    // 子任务允许的重试次数
    private int maxRetryTimes;
    // 子任务当前重试次数
    private int currentRetryTimes;

    public TaskTrackerStartTaskReq() {
    }

    public TaskTrackerStartTaskReq(JobInstanceInfo instanceInfo, TaskDO task) {
        jobId = instanceInfo.getJobId();
        instanceId = instanceInfo.getInstanceId();
        processorType = instanceInfo.getProcessorType();
        processorInfo = instanceInfo.getProcessorInfo();
        threadConcurrency = instanceInfo.getThreadConcurrency();
        executeType = instanceInfo.getExecuteType();
        taskTrackerAddress = NetUtils.getLocalHost();
        jobTimeLimitMS = instanceInfo.getTimeLimit();

        jobParams = instanceInfo.getJobParams();
        instanceParams = instanceInfo.getInstanceParams();

        taskName = task.getTaskName();
        subTaskContent = task.getTaskContent();

        maxRetryTimes = instanceInfo.getTaskRetryNum();
        currentRetryTimes = task.getFailedCnt();
    }
}
