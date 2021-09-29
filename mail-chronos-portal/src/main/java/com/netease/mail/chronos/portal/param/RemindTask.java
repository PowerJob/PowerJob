package com.netease.mail.chronos.portal.param;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author Echo009
 * @since 2021/9/21
 */
@Data
@Accessors(chain = true)
public class RemindTask {

    private String originId;

    private String uid;
    /**
     * iCalendar 重复规则
     */
    private String recurrenceRule;
    /**
     * 时区 ID
     */
    private String timeZoneId;
    /**
     * 下次触发时间
     */
    private Long nextTriggerTime;

    private String param;

    private Integer timesLimit;

    private Long startTime;

    private Long endTime;

}
