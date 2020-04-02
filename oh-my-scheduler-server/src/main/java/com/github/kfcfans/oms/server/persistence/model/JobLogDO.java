package com.github.kfcfans.oms.server.persistence.model;

import javax.persistence.*;
import java.util.Date;

/**
 * 任务运行日志表
 *
 * @author tjq
 * @since 2020/3/30
 */
@Entity
@Table(name = "job_log")
public class JobLogDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 任务ID
    private Long jobId;
    // 任务实例ID
    private String instanceId;
    // 任务状态 运行中/成功/失败...
    private int status;
    // 执行结果
    private String result;
    // 耗时
    private Long usedTime;

    private Date gmtCreate;
    private Date gmtModified;

}
