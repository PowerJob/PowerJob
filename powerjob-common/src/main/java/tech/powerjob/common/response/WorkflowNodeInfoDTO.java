package tech.powerjob.common.response;

import lombok.Data;

import java.util.Date;

/**
 * WorkflowNodeInfo 对外输出对象
 * @author Echo009
 * @since 2021/2/20
 */
@Data
public class WorkflowNodeInfoDTO {

    private Long id;

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
     * 是否启用
     */
    private Boolean enable;
    /**
     * 是否允许失败跳过
     */
    private Boolean skipWhenFailed;
    /**
     * 创建时间
     */
    private Date gmtCreate;
    /**
     * 更新时间
     */
    private Date gmtModified;


}
