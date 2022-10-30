package tech.powerjob.server.web.request;

import lombok.Data;

/**
 * 查询工作流
 *
 * @author tjq
 * @since 2020/5/27
 */
@Data
public class QueryWorkflowInfoRequest {

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
     * 查询条件
     */
    private Long workflowId;
    private String keyword;

}
