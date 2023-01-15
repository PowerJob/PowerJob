package tech.powerjob.server.persistence.remote.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

/**
 * 工作流运行实例表
 *
 * @author tjq
 * @since 2020/5/26
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(
        uniqueConstraints = {@UniqueConstraint(name = "uidx01_wf_instance", columnNames = {"wfInstanceId"})},
        indexes = {
                @Index(name = "idx01_wf_instance", columnList = "workflowId,status"),
                @Index(name = "idx01_wf_instance", columnList = "appId,status,expectedTriggerTime")
        }
)
public class WorkflowInstanceInfoDO {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private Long id;
    /**
     * 任务所属应用的ID，冗余提高查询效率
     */
    private Long appId;
    /**
     * workflowInstanceId（任务实例表都使用单独的ID作为主键以支持潜在的分表需求）
     */
    private Long wfInstanceId;
    /**
     * 上层工作流实例 ID （用于支持工作流嵌套）
     */
    private Long parentWfInstanceId;

    private Long workflowId;
    /**
     * workflow 状态（WorkflowInstanceStatus）
     */
    private Integer status;
    /**
     * 工作流启动参数
     */
    @Lob
    @Column
    private String wfInitParams;
    /**
     * 工作流上下文
     */
    @Lob
    @Column
    private String wfContext;

    @Lob
    @Column
    private String dag;

    @Lob
    @Column
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
