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
    @Column(columnDefinition = "bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID'")
    private Long id;

    /* ************************** 任务基本信息 ************************** */
    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '任务名称'")
    private String jobName;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '任务描述'")
    private String jobDescription;

    @Column(columnDefinition = "bigint DEFAULT NULL COMMENT '任务所属的应用ID'")
    private Long appId;

    @Lob
    @Column(columnDefinition = "longtext COMMENT '任务自带的参数'")
    private String jobParams;

    /* ************************** 定时参数 ************************** */
    @Column(columnDefinition = "int DEFAULT NULL COMMENT '时间表达式类型（CRON/API/FIX_RATE/FIX_DELAY）'")
    private Integer timeExpressionType;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '时间表达式，CRON/NULL/LONG/LONG'")
    private String timeExpression;

    /* ************************** 执行方式 ************************** */
    @Column(columnDefinition = "int DEFAULT NULL COMMENT '执行类型，单机/广播/MR'")
    private Integer executeType;

    @Column(columnDefinition = "int DEFAULT NULL COMMENT '执行器类型，Java/Shell'")
    private Integer processorType;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '执行器信息'")
    private String processorInfo;

    /* ************************** 运行时配置 ************************** */
    @Column(columnDefinition = "int DEFAULT NULL COMMENT '最大同时运行任务数，默认 1'")
    private Integer maxInstanceNum;

    @Column(columnDefinition = "int DEFAULT NULL COMMENT '并发度，同时执行某个任务的最大线程数量'")
    private Integer concurrency;

    @Column(columnDefinition = "bigint DEFAULT NULL COMMENT '任务整体超时时间'")
    private Long instanceTimeLimit;

    /* ************************** 重试配置 ************************** */

    @Column(columnDefinition = "int DEFAULT NULL COMMENT '实例重试次数'")
    private Integer instanceRetryNum;

    @Column(columnDefinition = "int DEFAULT NULL COMMENT '任务重试次数'")
    private Integer taskRetryNum;

    @Column(columnDefinition = "int DEFAULT NULL COMMENT '1 正常运行，2 停止（不再调度）'")
    private Integer status;

    @Column(columnDefinition = "bigint DEFAULT NULL COMMENT '下一次调度时间'")
    private Long nextTriggerTime;
    /* ************************** 繁忙机器配置 ************************** */
    @Column(columnDefinition = "double NOT NULL DEFAULT 0 COMMENT '最低CPU核心数量，0代表不限'")
    private double minCpuCores;

    @Column(columnDefinition = "double NOT NULL DEFAULT 0 COMMENT '最低内存空间，单位 GB，0代表不限'")
    private double minMemorySpace;

    @Column(columnDefinition = "double NOT NULL DEFAULT 0 COMMENT '最低磁盘空间，单位 GB，0代表不限'")
    private double minDiskSpace;
    /* ************************** 集群配置 ************************** */
    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '指定机器运行，空代表不限，非空则只会使用其中的机器运行（多值逗号分割）'")
    private String designatedWorkers;

    @Column(columnDefinition = "int DEFAULT NULL COMMENT '最大机器数量'")
    private Integer maxWorkerCount;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '报警用户ID列表，多值逗号分隔'")
    private String notifyUserIds;

    @Column(columnDefinition = "datetime(6) DEFAULT NULL COMMENT '创建时间'")
    private Date gmtCreate;

    @Column(columnDefinition = "datetime(6) DEFAULT NULL COMMENT '更新时间'")
    private Date gmtModified;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '扩展参数，PowerJob 自身不会使用该数据，留给开发者扩展时使用'")
    private String extra;

    @Column(columnDefinition = "int DEFAULT NULL COMMENT '派发策略'")
    private Integer dispatchStrategy;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '某种派发策略背后的具体配置，值取决于 dispatchStrategy'")
    private String dispatchStrategyConfig;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '生命周期'")
    private String lifecycle;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '告警配置'")
    private String alarmConfig;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '任务归类，开放给接入方自由定制'")
    private String tag;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '日志配置，包括日志级别、日志方式等配置信息'")
    private String logConfig;

    @Column(columnDefinition = "varchar(255) DEFAULT NULL COMMENT '高级运行时配置'")
    private String advancedRuntimeConfig;
}
