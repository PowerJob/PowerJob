package com.netease.mail.chronos.base.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Echo009
 * @since 2021/9/18
 */
@Getter
@RequiredArgsConstructor
public enum BaseStatusEnum implements StatusEnum {
    /**
     * 常见状态枚举
     * 遵循 http 状态码前缀定义
     */
    SUCCESS(200, "成功"),
    ILLEGAL_ARGUMENT(400, "参数错误"),
    ILLEGAL_ACCESS(401, "非法访问"),
    TOO_FREQUENTLY(402, "访问过于频繁"),
    UNKNOWN(500, "服务器异常");

    private final Integer code;

    private final String desc;


}
