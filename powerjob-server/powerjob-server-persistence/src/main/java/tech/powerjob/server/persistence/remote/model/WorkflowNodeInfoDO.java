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
    private Long id;

    @Column(nullable = false)
    private Long appId;

    @Column
    private Long workflowId;
    /**
     * 节点类型 {@link WorkflowNodeType}
     */
    private Integer type;
    /**
     * 任务 ID
     * 对于嵌套工作流类型的节点而言，这里存储是工作流 ID
     */
    private Long jobId;
    /**
     * 节点名称，默认为对应的任务名称
     */
    private String nodeName;
    /**
     * 节点参数
     */
    @Lob
    private String nodeParams;
    /**
     * 是否启用
     */
    @Column(nullable = false)
    private Boolean enable;
    /**
     * 是否允许失败跳过
     */
    @Column(nullable = false)
    private Boolean skipWhenFailed;

    @Lob
    private String extra;
    /**
     * 创建时间
     */
    @Column(nullable = false)
    private Date gmtCreate;
    /**
     * 更新时间
     */
    @Column(nullable = false)
    private Date gmtModified;


}
