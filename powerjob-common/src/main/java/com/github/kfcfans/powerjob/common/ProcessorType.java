package com.github.kfcfans.powerjob.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 处理器类型
 *
 * @author tjq
 * @since 2020/3/23
 */
@Getter
@AllArgsConstructor
public enum ProcessorType {

    EMBEDDED_JAVA(1, "内置JAVA处理器"),
    SHELL(2, "SHELL脚本"),
    PYTHON(3, "Python脚本"),
    JAVA_CONTAINER(4, "Java容器");

    private int v;
    private String des;

    public static ProcessorType of(int v) {
        for (ProcessorType type : values()) {
            if (type.v == v) {
                return type;
            }
        }
        throw new IllegalArgumentException("unknown ProcessorType of " + v);
    }
}
