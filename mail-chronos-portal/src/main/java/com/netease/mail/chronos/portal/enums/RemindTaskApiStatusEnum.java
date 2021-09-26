package com.netease.mail.chronos.portal.enums;

import com.netease.mail.chronos.base.enums.StatusEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Echo009
 * @since 2021/9/18
 */
@Getter
@RequiredArgsConstructor
public enum RemindTaskApiStatusEnum implements StatusEnum {
    /**
     * 提醒任务的 API 返回枚举
     */
    ALREADY_EXISTS(1001, "任务已存在，请勿重复创建！"),
    ;

    private final Integer code;

    private final String desc;


}
