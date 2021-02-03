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
public class ModifyWorkflowNodeRequest {

    private Long id;

    private Long appId;

    private Long workflowId;

    /**
     * 节点别名，默认为对应的任务名称
     */
    private String nodeAlias;
    /**
     * 节点参数
     */
    private String nodeParams;
    /**
     * 是否启用
     */
    private Boolean enable;
    /**
     * 是否允许失败跳过
     */
    private Boolean skipWhenFailed;


    public void valid(){
        CommonUtils.requireNonNull(this.id, "id can't be empty");
        CommonUtils.requireNonNull(this.appId, "appId can't be empty");
        CommonUtils.requireNonNull(this.nodeAlias, "nodeAlias can't be empty");
        CommonUtils.requireNonNull(this.enable, "enable can't be empty");
        CommonUtils.requireNonNull(this.skipWhenFailed, "skipWhenFailed can't be empty");
    }
}
