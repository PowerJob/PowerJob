package com.netease.mail.chronos.portal.entity.base;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 任务表
 *
 * @author job_info
 */
@TableName(value = "job_info")
@Data
public class JobInfo implements Serializable {
    /**
     *
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 应用ID
     */
    private Long appId;

    /**
     * 任务名称
     */
    private String jobName;

    /**
     * 任务描述
     */
    private String jobDescription;

    /**
     * 任务默认参数
     */
    private String jobParams;

    /**
     * 并发度,同时执行某个任务的最大线程数量
     */
    private Integer concurrency;

    /**
     * 运行节点,空:不限(多值逗号分割)
     */
    private String designatedWorkers;

    /**
     * 投递策略,1:健康优先/2:随机
     */
    private Integer dispatchStrategy;

    /**
     * 执行类型,1:单机STANDALONE/2:广播BROADCAST/3:MAP_REDUCE/4:MAP
     */
    private Integer executeType;

    /**
     * Instance重试次数
     */
    private Integer instanceRetryNum;

    /**
     * 任务整体超时时间
     */
    private Long instanceTimeLimit;

    /**
     * 生命周期
     */
    private String lifecycle;

    /**
     * 最大同时运行任务数,默认 1
     */
    private Integer maxInstanceNum;

    /**
     * 最大运行节点数量
     */
    private Integer maxWorkerCount;

    /**
     * 最低CPU核心数量,0:不限
     */
    private Double minCpuCores;

    /**
     * 最低磁盘空间(GB),0:不限
     */
    private Double minDiskSpace;

    /**
     * 最低内存空间(GB),0:不限
     */
    private Double minMemorySpace;

    /**
     * 下一次调度时间
     */
    private Long nextTriggerTime;

    /**
     * 报警用户(多值逗号分割)
     */
    private String notifyUserIds;

    /**
     * 执行器信息
     */
    private String processorInfo;

    /**
     * 执行器类型,1:内建处理器BUILT_IN/2:SHELL/3:PYTHON/4:外部处理器（动态加载）EXTERNAL
     */
    private Integer processorType;

    /**
     * 状态,1:正常ENABLE/2:已禁用DISABLE/99:已删除DELETED
     */
    private Integer status;

    /**
     * Task重试次数
     */
    private Integer taskRetryNum;

    /**
     * 时间表达式,内容取决于time_expression_type,1:CRON/2:NULL/3:LONG/4:LONG
     */
    private String timeExpression;

    /**
     * 时间表达式类型,1:CRON/2:API/3:FIX_RATE/4:FIX_DELAY,5:WORKFLOW
     * ）
     */
    private Integer timeExpressionType;

    /**
     * 扩展字段
     */
    private String extra;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 更新时间
     */
    private Date gmtModified;


    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}