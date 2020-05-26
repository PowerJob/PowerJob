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

    private Long workflowId;

    // workflow 状态（WorkflowInstanceStatus）
    private Integer status;

    private String dag;

    private String result;

    private Date gmtCreate;
    private Date gmtModified;
}
