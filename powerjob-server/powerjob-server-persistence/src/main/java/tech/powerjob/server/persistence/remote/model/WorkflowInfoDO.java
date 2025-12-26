package tech.powerjob.server.persistence.remote.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

/**
 * DAG 工作流信息表
 *
 * @author tjq
 * @since 2020/5/26
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(indexes = {
        @Index(name = "idx01_workflow_info",columnList = "appId,status,timeExpressionType,nextTriggerTime")
})
public class WorkflowInfoDO {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    @Column(columnDefinition = "bigint NOT NULL AUTO_INCREMENT COMMENT '工作流ID'")
    private Long id;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '工作流名称'")
    private String wfName;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '工作流描述'")
    private String wfDescription;

    @Column(columnDefinition = "bigint DEFAULT NULL COMMENT '所属应用ID'")
    private Long appId;

    @Lob
    @Column(columnDefinition = "longtext COMMENT '工作流的DAG图信息（点线式DAG的json）'")
    private String peDAG;

    /* ************************** 定时参数 ************************** */
    @Column(columnDefinition = "int DEFAULT NULL COMMENT '时间表达式类型（CRON/API/FIX_RATE/FIX_DELAY）'")
    private Integer timeExpressionType;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '时间表达式，CRON/NULL/LONG/LONG'")
    private String timeExpression;

    @Column(columnDefinition = "int DEFAULT NULL COMMENT '最大同时运行的工作流个数，默认 1'")
    private Integer maxWfInstanceNum;

    @Column(columnDefinition = "int DEFAULT NULL COMMENT '1 正常运行，2 停止（不再调度）'")
    private Integer status;

    @Column(columnDefinition = "bigint DEFAULT NULL COMMENT '下一次调度时间'")
    private Long nextTriggerTime;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '工作流整体失败的报警'")
    private String notifyUserIds;

    @Column(columnDefinition = "datetime(6) DEFAULT NULL COMMENT '创建时间'")
    private Date gmtCreate;

    @Column(columnDefinition = "datetime(6) DEFAULT NULL COMMENT '更新时间'")
    private Date gmtModified;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '扩展字段'")
    private String extra;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '生命周期'")
    private String lifecycle;
}