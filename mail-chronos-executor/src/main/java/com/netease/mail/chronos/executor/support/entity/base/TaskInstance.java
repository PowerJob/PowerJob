package com.netease.mail.chronos.executor.support.entity.base;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 任务实例
 * @author Echo009
 */
@Data
public class TaskInstance implements Serializable {
    /**
     * 注意这个类不用能 baseMapper 提供的一系列方法
     * 实际上的主键是 id + partitionKey 
     */
    @TableId
    private Long id;


    private Integer partitionKey;

    /**
     * 任务 ID
     */
    private Long taskId;

    /**
     * 业务方定义的 ID，带非唯一索引
     */
    private String customId;
    /**
     * 业务方定义的 key ，带非唯一索引
     */
    private String customKey;

    /**
     * 任务参数
     */
    private String param;

    /**
     * 附加信息（JSON）
     */
    private String extra;

    /**
     * 期望触发时间
     */
    private Long expectedTriggerTime;

    /**
     * 实际触发时间（记录的是首次执行时间）
     */
    private Long actualTriggerTime;

    /**
     * 完成时间
     */
    private Long finishedTime;

    /**
     * 运行次数
     */
    private Integer runningTimes;

    /**
     * 最大重试次数,< 0 代表不限
     */
    private Integer maxRetryTimes;

    /**
     * 执行结果(取决于业务逻辑)
     */
    private String result;

    /**
     * 状态(执行状态)
     */
    private Integer status;

    /**
     * 是否启用，失败且不需要重试，或者手动停止的这个状态会为置为 0 
     */
    private Boolean enable;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 创建时间
     */
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}