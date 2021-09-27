package com.netease.mail.chronos.executor.support.po;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author Echo009
 * @since 2021/9/27
 */
@Data
@Accessors(chain = true)
public class SpRemindTaskSimpleInfo {

    private Long id;

    private Long nextTriggerTime;
}
