package com.github.kfcfans.oms.worker.pojo.request;

import com.github.kfcfans.common.OmsSerializable;
import lombok.Data;


/**
 * 广播任务 preExecute 结束信息
 *
 * @author tjq
 * @since 2020/3/23
 */
@Data
public class BroadcastTaskPreExecuteFinishedReq implements OmsSerializable {

    private Long instanceId;
    private Long subInstanceId;
    private String taskId;

    private boolean success;
    private String msg;

    // 上报时间
    private long reportTime;
}
