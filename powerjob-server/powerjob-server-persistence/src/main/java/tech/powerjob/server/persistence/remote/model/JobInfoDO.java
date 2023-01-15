package tech.powerjob.server.persistence.remote.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

/**
 * 任务信息表
 *
 * @author tjq
 * @since 2020/3/29
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(indexes = {
        @Index(name = "idx01_job_info", columnList = "appId,status,timeExpressionType,nextTriggerTime"),
})
public class JobInfoDO {


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private Long id;

    /* ************************** 任务基本信息 ************************** */
    /**
     * 任务名称
     */
    private String jobName;
    /**
     * 任务描述
     */
    private String jobDescription;
    /**
     * 任务所属的应用ID
     */
    private Long appId;
    /**
     * 任务自带的参数
     */
    @Lob
    @Column
    private String jobParams;

    /* ************************** 定时参数 ************************** */
    /**
     * 时间表达式类型（CRON/API/FIX_RATE/FIX_DELAY）
     */
    private Integer timeExpressionType;
    /**
     * 时间表达式，CRON/NULL/LONG/LONG
     */
    private String timeExpression;

    /* ************************** 执行方式 ************************** */
    /**
     * 执行类型，单机/广播/MR
     */
    private Integer executeType;
    /**
     * 执行器类型，Java/Shell
     */
    private Integer processorType;
    /**
     * 执行器信息
     */
    private String processorInfo;

    /* ************************** 运行时配置 ************************** */
    /**
     * 最大同时运行任务数，默认 1
     */
    private Integer maxInstanceNum;
    /**
     * 并发度，同时执行某个任务的最大线程数量
     */
    private Integer concurrency;
    /**
     * 任务整体超时时间
     */
    private Long instanceTimeLimit;

    /* ************************** 重试配置 ************************** */

    private Integer instanceRetryNum;

    private Integer taskRetryNum;

    /**
     * 1 正常运行，2 停止（不再调度）
     */
    private Integer status;
    /**
     * 下一次调度时间
     */
    private Long nextTriggerTime;
    /* ************************** 繁忙机器配置 ************************** */
    /**
     * 最低CPU核心数量，0代表不限
     */
    private double minCpuCores;
    /**
     * 最低内存空间，单位 GB，0代表不限
     */
    private double minMemorySpace;
    /**
     * 最低磁盘空间，单位 GB，0代表不限
     */
    private double minDiskSpace;
    /* ************************** 集群配置 ************************** */
    /**
     * 指定机器运行，空代表不限，非空则只会使用其中的机器运行（多值逗号分割）
     */
    private String designatedWorkers;
    /**
     * 最大机器数量
     */
    private Integer maxWorkerCount;
    /**
     * 报警用户ID列表，多值逗号分隔
     */
    private String notifyUserIds;

    private Date gmtCreate;

    private Date gmtModified;

    /**
     * 扩展参数，PowerJob 自身不会使用该数据，留给开发者扩展时使用
     * 比如 WorkerFilter 的自定义 worker 过滤逻辑，可在此传入过滤指标 GpuUsage < 10
     */
    private String extra;

    private Integer dispatchStrategy;

    private String lifecycle;
    /**
     * 告警配置
     */
    private String alarmConfig;

    /**
     * 任务归类，开放给接入方自由定制
     */
    private String tag;

    /**
     * 日志配置，包括日志级别、日志方式等配置信息
     */
    private String logConfig;
}
