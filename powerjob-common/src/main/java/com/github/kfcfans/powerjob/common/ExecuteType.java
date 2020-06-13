package com.github.kfcfans.powerjob.common;

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
    STANDALONE(1, "单机执行"),
    BROADCAST(2, "广播执行"),
    MAP_REDUCE(3, "MapReduce"),
    MAP(4, "Map");

    int v;
    String des;

    public static ExecuteType of(int v) {
        for (ExecuteType type : values()) {
            if (type.v == v) {
                return type;
            }
        }
        throw new IllegalArgumentException("unknown ExecuteType of " + v);
    }
}
