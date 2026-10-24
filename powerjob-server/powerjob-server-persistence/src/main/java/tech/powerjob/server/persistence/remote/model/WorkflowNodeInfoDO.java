package tech.powerjob.server.persistence.remote.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import tech.powerjob.common.enums.WorkflowNodeType;

import javax.persistence.*;
import java.util.Date;

/**
 * 工作流节点信息
 * 记录了工作流中的任务节点个性化的配置信息
 *
 * @author Echo009
 * @since 2021/1/23
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(indexes = {
        @Index(name = "idx01_workflow_node_info", columnList = "workflowId,gmtCreate")
})
public class WorkflowNodeInfoDO {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    @Column(columnDefinition = "bigint NOT NULL AUTO_INCREMENT COMMENT '节点ID'")
    private Long id;

    @Column(columnDefinition = "bigint NOT NULL COMMENT '所属的应用ID'")
    private Long appId;

    @Column(columnDefinition = "bigint DEFAULT NULL COMMENT '工作流ID'")
    private Long workflowId;
    /**
     * 节点类型 {@link WorkflowNodeType}
     */
    @Column(columnDefinition = "int DEFAULT NULL COMMENT '节点类型'")
    private Integer type;
    /**
     * 任务 ID
     * 对于嵌套工作流类型的节点而言，这里存储是工作流 ID
     */
    @Column(columnDefinition = "bigint DEFAULT NULL COMMENT '任务 ID'")
    private Long jobId;
    /**
     * 节点名称，默认为对应的任务名称
     */
    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '节点名称，默认为对应的任务名称'")
    private String nodeName;
    /**
     * 节点参数
     */
    @Lob
    @Column(columnDefinition = "longtext COMMENT '节点参数'")
    private String nodeParams;
    /**
     * 是否启用
     */
    @Column(columnDefinition = "bit(1) NOT NULL DEFAULT 0 COMMENT '是否启用'")
    private Boolean enable;
    /**
     * 是否允许失败跳过
     */
    @Column(columnDefinition = "bit(1) NOT NULL DEFAULT 0 COMMENT '是否允许失败跳过'")
    private Boolean skipWhenFailed;

    @Lob
    @Column(columnDefinition = "longtext COMMENT '扩展字段'")
    private String extra;
    /**
     * 创建时间
     */
    @Column(columnDefinition = "datetime(6) NOT NULL COMMENT '创建时间'")
    private Date gmtCreate;
    /**
     * 更新时间
     */
    @Column(columnDefinition = "datetime(6) NOT NULL COMMENT '更新时间'")
    private Date gmtModified;


}