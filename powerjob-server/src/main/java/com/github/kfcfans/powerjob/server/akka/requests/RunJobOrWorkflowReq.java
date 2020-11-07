package com.github.kfcfans.powerjob.server.akka.requests;

import com.github.kfcfans.powerjob.common.OmsSerializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 运行 Job 或 工作流，需要转发到 server 进行，否则没有集群信息
 *
 * @author tjq
 * @since 11/7/20
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RunJobOrWorkflowReq implements OmsSerializable {
    public static final int JOB = 1;
    public static final int WORKFLOW = 2;

    private int type;
    private long id;
    private long delay;
    private String params;

    private long appId;
}
