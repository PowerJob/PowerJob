package com.netease.mail.chronos.portal.client.param;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * @author Echo009
 * @since 2021/9/18
 */
@Data
@Accessors(chain = true)
@Builder
public class RemindTask {
    /**
     * 集合 ID
     */
    private String colId;
    /**
     * 组件 ID
     *
     * 注意: compId 需保证唯一
     */
    private String compId;
    /**
     * Uid
     */
    private String uid;
    /**
     * iCalendar 重复规则
     * 会从这里解析出 任务的结束时间，重复次数
     */
    private String recurrenceRule;
    /**
     * 种子时间
     * 考虑到创建任务的时延，这个时间必须大于当前时间 60 s 以上
     * 也就是说支持的最短延时任务为 1 分钟
     */
    private Long seedTime;
    /**
     * 触发偏移，最终的触发时间都会加上这个值
     */
    private List<Long> triggerOffsets;
    /**
     * 时区 Id
     */
    private String timeZoneId;
    /**
     * 任务参数，不对此字段做处理
     */
    private String param;
    /**
     * 其他信息，预留
     */
    private Map<String, Object> extra;


}
