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
                @Index(name = "idx02_wf_instance", columnList = "appId,status,expectedTriggerTime")
        }
)
public class WorkflowInstanceInfoDO {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    @Column(columnDefinition = "bigint NOT NULL AUTO_INCREMENT COMMENT '工作流实例ID'")
    private Long id;

    @Column(columnDefinition = "bigint DEFAULT NULL COMMENT '任务所属应用的ID，冗余提高查询效率'")
    private Long appId;

    @Column(columnDefinition = "bigint DEFAULT NULL COMMENT 'workflowInstanceId（任务实例表都使用单独的ID作为主键以支持潜在的分表需求）'")
    private Long wfInstanceId;

    @Column(columnDefinition = "bigint DEFAULT NULL COMMENT '上层工作流实例 ID （用于支持工作流嵌套）'")
    private Long parentWfInstanceId;

    @Column(columnDefinition = "bigint DEFAULT NULL COMMENT '工作流ID'")
    private Long workflowId;

    @Column(columnDefinition = "int DEFAULT NULL COMMENT 'workflow 状态（WorkflowInstanceStatus）'")
    private Integer status;

    @Lob
    @Column(columnDefinition = "longtext COMMENT '工作流启动参数'")
    private String wfInitParams;

    @Lob
    @Column(columnDefinition = "longtext COMMENT '工作流上下文'")
    private String wfContext;

    @Lob
    @Column(columnDefinition = "longtext COMMENT 'DAG信息'")
    private String dag;

    @Lob
    @Column(columnDefinition = "longtext COMMENT '执行结果'")
    private String result;

    @Column(columnDefinition = "bigint DEFAULT NULL COMMENT '预计触发时间'")
    private Long expectedTriggerTime;

    @Column(columnDefinition = "bigint DEFAULT NULL COMMENT '实际触发时间'")
    private Long actualTriggerTime;

    @Column(columnDefinition = "bigint DEFAULT NULL COMMENT '结束时间'")
    private Long finishedTime;

    @Column(columnDefinition = "datetime(6) DEFAULT NULL COMMENT '创建时间'")
    private Date gmtCreate;

    @Column(columnDefinition = "datetime(6) DEFAULT NULL COMMENT '更新时间'")
    private Date gmtModified;
}