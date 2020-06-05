package com.github.kfcfans.oms.server.persistence.core.model;

import com.github.kfcfans.oms.common.InstanceStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@Table(name = "instance_info", indexes = {@Index(columnList = "jobId"), @Index(columnList = "appId"), @Index(columnList = "instanceId")})
public class InstanceInfoDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 任务ID
    private Long jobId;
    // 任务所属应用的ID，冗余提高查询效率
    private Long appId;
    // 任务实例ID
    private Long instanceId;
    // 任务实例参数
    @Lob
    @Column(columnDefinition="TEXT")
    private String instanceParams;

    // 该任务实例的类型，普通/工作流（InstanceType）
    private Integer type;
    // 该任务实例所属的 workflow ID，仅 workflow 任务存在
    private Long wfInstanceId;
    /**
     * 任务状态 {@link InstanceStatus}
     */
    private Integer status;
    // 执行结果（允许存储稍大的结果）
    @Lob
    @Column(columnDefinition="TEXT")
    private String result;
    // 预计触发时间
    private Long expectedTriggerTime;
    // 实际触发时间
    private Long actualTriggerTime;
    // 结束时间
    private Long finishedTime;
    // TaskTracker地址
    private String taskTrackerAddress;

    // 总共执行的次数（用于重试判断）
    private Long runningTimes;

    private Date gmtCreate;
    private Date gmtModified;

}
