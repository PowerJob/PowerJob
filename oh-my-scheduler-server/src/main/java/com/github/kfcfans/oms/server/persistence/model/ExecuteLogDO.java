package com.github.kfcfans.oms.server.persistence.model;

import lombok.Data;

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
@Table(name = "execute_log", indexes = {@Index(columnList = "jobId")})
public class ExecuteLogDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 任务ID
    private Long jobId;
    // 任务实例ID
    private Long instanceId;
    /**
     * 任务状态 {@link com.github.kfcfans.common.InstanceStatus}
     */
    private int status;
    // 执行结果
    private String result;
    // 耗时
    private Long usedTime;
    // 预计触发时间
    private Long expectedTriggerTime;
    // 实际触发时间
    private Long actualTriggerTime;

    private Date gmtCreate;
    private Date gmtModified;

}
