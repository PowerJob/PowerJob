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
public enum InstanceStatus {
    RUNNING(3, "运行中"),
    SUCCEED(4, "运行成功"),
    FAILED(5, "运行失败");

    private int value;
    private String des;
}
