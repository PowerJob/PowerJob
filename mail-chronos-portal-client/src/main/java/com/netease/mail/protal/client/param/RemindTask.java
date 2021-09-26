package com.netease.mail.protal.client.param;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author Echo009
 * @since 2021/9/18
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
    /**
     * 触发次数限制，小于等于 0 表示不限次数
     */
    private Integer timesLimit;

    private Long startTime;

    private Long endTime;

}
