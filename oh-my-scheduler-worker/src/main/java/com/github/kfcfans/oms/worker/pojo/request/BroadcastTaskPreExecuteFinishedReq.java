package com.github.kfcfans.oms.worker.pojo.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 广播任务 preExecute 结束信息
 *
 * @author tjq
 * @since 2020/3/23
 */
@Data
public class BroadcastTaskPreExecuteFinishedReq implements Serializable {

    private Long instanceId;
    private String taskId;

    private boolean success;
    private String msg;
}
