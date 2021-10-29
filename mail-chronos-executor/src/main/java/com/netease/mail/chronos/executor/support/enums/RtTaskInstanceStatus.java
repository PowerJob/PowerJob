package com.netease.mail.chronos.executor.support.enums;

import com.netease.mail.chronos.base.enums.CodeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Echo009
 * @since 2021/10/28
 */
@AllArgsConstructor
@Getter
public enum RtTaskInstanceStatus implements CodeEnum<Integer> {
    /**
     * 实例状态
     */
    INIT(0,"初始状态，等待执行"),
    /**
     * 看情况而定，如果执行时间不是很长（<10s），不建议使用这个状态
     */
    RUNNING(1,"正在执行"),
    FAILED(2,"执行失败"),
    SUCCESS(3,"执行成功"),
    ;

    private final Integer code;

    private final String desc;





}
