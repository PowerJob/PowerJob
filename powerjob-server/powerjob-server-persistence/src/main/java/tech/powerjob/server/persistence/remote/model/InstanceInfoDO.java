package tech.powerjob.server.persistence.remote.model;

import tech.powerjob.common.enums.InstanceStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

/**
 * 任务运行日志表
 *
 * @author tjq
 * @since 2020/3/30
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(indexes = {
        @Index(name = "idx01_instance_info", columnList = "jobId,status"),
        @Index(name = "idx02_instance_info", columnList = "appId,status"),
        @Index(name = "idx03_instance_info", columnList = "instanceId,status"),
        @Index(name = "idx04_instance_info_outer_key", columnList = "outerKey")
})
public class InstanceInfoDO {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    @Column(columnDefinition = "bigint NOT NULL AUTO_INCREMENT COMMENT '任务实例ID'")
    private Long id;

    @Column(columnDefinition = "bigint DEFAULT NULL COMMENT '任务ID'")
    private Long jobId;

    @Column(columnDefinition = "bigint DEFAULT NULL COMMENT '任务所属应用的ID，冗余提高查询效率'")
    private Long appId;

    @Column(columnDefinition = "bigint DEFAULT NULL COMMENT '任务所属应用的ID，冗余提高查询效率'")
    private Long instanceId;

    @Lob
    @Column(columnDefinition = "longtext COMMENT '任务参数（静态）'")
    private String jobParams;

    @Lob
    @Column(columnDefinition = "longtext COMMENT '任务实例参数（动态）'")
    private String instanceParams;

    @Column(columnDefinition = "int DEFAULT NULL COMMENT '该任务实例的类型，普通/工作流（InstanceType）'")
    private Integer type;

    @Column(columnDefinition = "bigint DEFAULT NULL COMMENT '该任务实例所属的 workflow ID，仅 workflow 任务存在'")
    private Long wfInstanceId;

    @Column(columnDefinition = "int DEFAULT NULL COMMENT '任务状态'")
    private Integer status;

    @Lob
    @Column(columnDefinition = "longtext COMMENT '执行结果（允许存储稍大的结果）'")
    private String result;

    @Column(columnDefinition = "bigint DEFAULT NULL COMMENT '预计触发时间'")
    private Long expectedTriggerTime;

    @Column(columnDefinition = "bigint DEFAULT NULL COMMENT '实际触发时间'")
    private Long actualTriggerTime;

    @Column(columnDefinition = "bigint DEFAULT NULL COMMENT '结束时间'")
    private Long finishedTime;

    @Column(columnDefinition = "bigint DEFAULT NULL COMMENT '最后上报时间'")
    private Long lastReportTime;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT 'TaskTracker 地址'")
    private String taskTrackerAddress;

    @Column(columnDefinition = "bigint DEFAULT NULL COMMENT '总共执行的次数（用于重试判断）'")
    private Long runningTimes;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '“外键”，用于 OPENAPI 场景业务场景与 PowerJob 实例的绑定'")
    private String outerKey;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '扩展属性，用于 OPENAPI 场景上下文参数的透传'")
    private String extendValue;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '调度元信息'")
    private String meta;

    @Column(columnDefinition = "datetime(6) DEFAULT NULL COMMENT '创建时间'")
    private Date gmtCreate;

    @Column(columnDefinition = "datetime(6) DEFAULT NULL COMMENT '更新时间'")
    private Date gmtModified;

}