package com.github.kfcfans.common;

import lombok.AllArgsConstructor;

/**
 * 处理器类型
 *
 * @author tjq
 * @since 2020/3/23
 */
@AllArgsConstructor
public enum ProcessorType {

    EMBEDDED_JAVA("内置Java对象");

    private String des;
}
