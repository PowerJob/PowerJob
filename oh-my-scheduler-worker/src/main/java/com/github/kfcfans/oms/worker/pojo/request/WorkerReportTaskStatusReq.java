package com.github.kfcfans.oms.worker.pojo.request;

import lombok.Data;

/**
 * worker 上报 task 执行情况
 *
 * @author tjq
 * @since 2020/3/17
 */
@Data
public class WorkerReportTaskStatusReq {

    private String jobId;
    private String instanceId;
    private String taskId;

    private int status;
    /**
     * 执行完成时才有
     */
    private String result;

}
