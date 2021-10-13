package com.netease.mail.chronos.portal.client.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @author Echo009
 * @since 2021/9/18
 */
@Data
@Accessors(chain = true)
public class RemindTaskVo {
    /**
     *
     */
    private Long id;
    /**
     * 集合 ID
     */
    private String colId;
    /**
     * 组件 ID
     */
    private String compId;

    /**
     * 用户ID
     */
    private String uid;

    /**
     * iCalendar 重复规则
     */
    private String recurrenceRule;

    /**
     * 原始触发偏移
     */
    private Long triggerOffset;

    /**
     * 任务参数
     */
    private String param;

    /**
     * 附加信息
     */
    private String extra;

    /**
     * 已触发次数
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


    private static final long serialVersionUID = 1L;


}
