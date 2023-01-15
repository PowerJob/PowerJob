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
    private Long id;

    private String wfName;

    private String wfDescription;

    /**
     * 所属应用ID
     */
    private Long appId;

    /**
     * 工作流的DAG图信息（点线式DAG的json）
     */
    @Lob
    @Column
    private String peDAG;

    /* ************************** 定时参数 ************************** */
    /**
     * 时间表达式类型（CRON/API/FIX_RATE/FIX_DELAY）
     */
    private Integer timeExpressionType;
    /**
     * 时间表达式，CRON/NULL/LONG/LONG
     */
    private String timeExpression;

    /**
     * 最大同时运行的工作流个数，默认 1
     */
    private Integer maxWfInstanceNum;

    /**
     * 1 正常运行，2 停止（不再调度）
     */
    private Integer status;
    /**
     * 下一次调度时间
     */
    private Long nextTriggerTime;
    /**
     * 工作流整体失败的报警
     */
    private String notifyUserIds;

    private Date gmtCreate;

    private Date gmtModified;

    private String extra;

    private String lifecycle;
}
