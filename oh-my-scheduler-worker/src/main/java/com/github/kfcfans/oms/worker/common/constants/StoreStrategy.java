package com.github.kfcfans.oms.worker.common.constants;

import lombok.AllArgsConstructor;

/**
 * 持久化策略
 *
 * @author tjq
 * @since 2020/4/14
 */
@AllArgsConstructor
public enum  StoreStrategy {

    DISK("磁盘"),
    MEMORY("内存");

    private String des;
}
