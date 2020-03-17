package com.github.kfcfans.oms.worker.pojo.request;

import com.github.kfcfans.oms.worker.common.utils.NetUtils;
import com.github.kfcfans.oms.worker.persistence.TaskDO;
import com.github.kfcfans.oms.worker.pojo.model.JobInstanceInfo;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JobTracker 派发 task 进行执行
 *
 * @author tjq
 * @since 2020/3/17
 */
@Data
@NoArgsConstructor
public class TaskTrackerStartTaskReq {

    private String jobId;
    private String instanceId;
    // 处理器类型（JavaBean、Jar、脚本等）
    private String processorType;
    // 处理器信息
    private String processorInfo;
    // 并发计算线程数
    private int threadConcurrency;
    // JobTracker 地址
    private String jobTrackerAddress;

    private String jobParams;
    private String instanceParams;

    private String taskName;
    private byte[] taskContent;
    // 子任务允许的重试次数
    private int taskRetryNum;
    // 子任务当前重试次数
    private int currentRetryNum;

    public TaskTrackerStartTaskReq(JobInstanceInfo instanceInfo, TaskDO task) {
        jobId = instanceInfo.getJobId();
        instanceId = instanceInfo.getInstanceId();
        processorType = instanceInfo.getProcessorType();
        processorInfo = instanceInfo.getProcessorInfo();
        threadConcurrency = instanceInfo.getThreadConcurrency();
        jobTrackerAddress = NetUtils.getLocalHost();

        jobParams = instanceInfo.getJobParams();
        instanceParams = instanceInfo.getInstanceParams();

        taskName = task.getTaskName();
        taskContent = task.getTaskContent();

        taskRetryNum = instanceInfo.getTaskRetryNum();
        currentRetryNum = task.getFailedCnt();
    }
}
