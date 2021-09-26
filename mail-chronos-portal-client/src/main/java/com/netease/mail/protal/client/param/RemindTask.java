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
    /**
     * 下次触发时间
     * 考虑到创建任务的时延，这个时间必须大于当前时间 60 s 以上
     * 也就是说支持的最短延时任务为 1 分钟
     */
    private Long nextTriggerTime;

    private String param;
    /**
     * 触发次数限制，小于等于 0 表示不限次数
     */
    private Integer timesLimit;
    /**
     * 非必传，默认为空，表示不限
     */
    private Long startTime;
    /**
     * 非必传，默认为空，表示不限
     */
    private Long endTime;

}
