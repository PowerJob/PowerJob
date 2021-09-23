package com.netease.mail.chronos.portal.entity.support;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 提醒任务信息
 * @author  sp_remind_task_info
 */
@TableName(value ="sp_remind_task_info")
@Data
@Accessors(chain = true)
public class SpRemindTaskInfo implements Serializable {
    /**
     * 
     */
    @TableId
    private Long id;

    /**
     * 原始 ID
     */
    private String originId;

    /**
     * 用户ID
     */
    private String uid;

    /**
     * cron 表达式
     */
    private String cron;


    private String timeZoneId;

    /**
     * 任务参数
     */
    private String param;

    /**
     * 附加信息
     */
    private String extra;

    /**
     * 触发时间
     */
    private Integer triggerTimes;

    /**
     * 次数限制
     */
    private Integer timesLimit;

    /**
     * 下次触发时间
     */
    private Long nextTriggerTime;

    /**
     * 开始时间
     */
    private Long startTime;

    /**
     * 结束时间
     */
    private Long endTime;

    /**
     * 是否启用
     */
    private Boolean enable;

    /**
     * 被禁用的时间
     */
    private Date disableTime;

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