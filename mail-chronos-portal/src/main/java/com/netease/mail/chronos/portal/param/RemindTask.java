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

    private String cron;
    /**
     * 时区 ID
     */
    private String timeZoneId;

    private String param;

    private Integer timesLimit;

    private Long startTime;

    private Long endTime;

}
