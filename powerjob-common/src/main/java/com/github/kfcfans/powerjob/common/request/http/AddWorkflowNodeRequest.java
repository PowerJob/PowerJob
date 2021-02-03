package com.github.kfcfans.powerjob.common.request.http;

import com.github.kfcfans.powerjob.common.utils.CommonUtils;
import lombok.Data;



/**
 * 新增工作流节点信息请求
 *
 * @author zenggonggu
 * @since 2021/02/02
 */
@Data
public class AddWorkflowNodeRequest {

    private Long appId;

    private Long workflowId;
    /**
     * 任务 ID
     */
    private Long jobId;
    /**
     * 节点别名，默认为对应的任务名称
     */
    private String nodeAlias;
    /**
     * 节点参数
     */
    private String nodeParams;
    /**
     * 是否启用，默认启用
     */
    private Boolean enable = true;
    /**
     * 是否允许失败跳过，默认不允许
     */
    private Boolean skipWhenFailed = false;

    public void valid(){
        CommonUtils.requireNonNull(this.appId, "appId can't be empty");
        CommonUtils.requireNonNull(this.workflowId, "workflowId can't be empty");
        CommonUtils.requireNonNull(this.jobId, "jobId can't be empty");
    }
}
