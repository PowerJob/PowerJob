package com.github.kfcfans.common;

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

    EMBEDDED_JAVA(1, "内置Java对象");

    private int v;
    private String des;
}
