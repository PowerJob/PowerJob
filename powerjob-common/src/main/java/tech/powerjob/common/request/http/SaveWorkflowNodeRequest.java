package tech.powerjob.common.request.http;

import tech.powerjob.common.enums.WorkflowNodeType;
import tech.powerjob.common.utils.CommonUtils;
import lombok.Data;



/**
 * 保存工作流节点信息请求
 * 工作流节点的
 *
 * @author zenggonggu
 * @since 2021/02/02
 */
@Data
public class SaveWorkflowNodeRequest {

    private Long id;

    private Long appId;
    /**
     * 节点类型(默认为任务节点)
     */
    private WorkflowNodeType type = WorkflowNodeType.JOB;
    /**
     * 任务 ID
     */
    private Long jobId;
    /**
     * 节点别名，默认为对应的任务名称
     */
    private String nodeName;
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
        CommonUtils.requireNonNull(this.type, "type can't be empty");
        if (type == WorkflowNodeType.JOB) {
            CommonUtils.requireNonNull(this.jobId, "jobId can't be empty");
        }
    }
}
