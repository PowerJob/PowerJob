package tech.powerjob.common.response;

import lombok.Data;

import java.util.Date;

/**
 * WorkflowInstanceInfo 对外输出对象
 *
 * @author tjq
 * @since 2020/6/2
 */
@Data
public class WorkflowInstanceInfoDTO {

    private Long id;
    private Long appId;

    private Long wfInstanceId;
    private Long workflowId;

    /**
     * workflow 状态（WorkflowInstanceStatus）
     */
    private Integer status;
    /**
     * 工作流启动参数
     */
    private String wfInitParams;
    /**
     * 工作流上下文
     */
    private String wfContext;

    private String dag;
    private String result;

    /**
     * 预计触发时间
     */
    private Long expectedTriggerTime;
    /**
     * 实际触发时间
     */
    private Long actualTriggerTime;
    /**
     * 结束时间
     */
    private Long finishedTime;

    private Date gmtCreate;
    private Date gmtModified;
}
