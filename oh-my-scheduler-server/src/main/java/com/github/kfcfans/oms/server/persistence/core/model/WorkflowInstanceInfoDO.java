package com.github.kfcfans.oms.server.persistence.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@Table(name = "workflow_instance_info")
public class WorkflowInstanceInfoDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // 任务所属应用的ID，冗余提高查询效率
    private Long appId;

    // workflowInstanceId（任务实例表都使用单独的ID作为主键以支持潜在的分表需求）
    private Long wfInstanceId;

    private Long workflowId;

    // workflow 状态（WorkflowInstanceStatus）
    private Integer status;

    @Lob
    @Column(columnDefinition="TEXT")
    private String dag;
    @Lob
    @Column(columnDefinition="TEXT")
    private String result;

    private Date gmtCreate;
    private Date gmtModified;
}
