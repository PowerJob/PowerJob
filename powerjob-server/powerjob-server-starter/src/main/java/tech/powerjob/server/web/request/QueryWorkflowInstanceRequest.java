package tech.powerjob.server.web.request;

import lombok.Data;

/**
 * 查询工作流实例请求
 *
 * @author tjq
 * @since 2020/5/31
 */
@Data
public class QueryWorkflowInstanceRequest {

    /**
     * 任务所属应用ID
     */
    private Long appId;
    /**
     * 当前页码
     */
    private Integer index;
    /**
     * 页大小
     */
    private Integer pageSize;
    /**
     * 查询条件（NORMAL/WORKFLOW）
     */
    private Long wfInstanceId;

    private Long workflowId;

    private String status;
}
