package com.github.kfcfans.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务执行类型
 *
 * @author tjq
 * @since 2020/3/17
 */
@Getter
@AllArgsConstructor
public enum ExecuteType {
    STANDALONE(1),
    BROADCAST(2),
    MAP_REDUCE(3);

    int v;
}
