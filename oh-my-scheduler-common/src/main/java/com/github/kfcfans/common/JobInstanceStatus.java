package com.github.kfcfans.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * description
 *
 * @author tjq
 * @since 2020/3/17
 */
@Getter
@AllArgsConstructor
public enum JobInstanceStatus {
    RUNNING(1, "运行中"),
    SUCCEED(2, "运行成功"),
    FAILED(3, "运行失败");

    private int value;
    private String des;
}
