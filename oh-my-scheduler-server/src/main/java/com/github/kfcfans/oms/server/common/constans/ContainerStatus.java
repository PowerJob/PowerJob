package com.github.kfcfans.oms.server.common.constans;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 容器状态
 * 由于命名约束，准备采取硬删除策略
 *
 * @author tjq
 * @since 2020/5/15
 */
@Getter
@AllArgsConstructor
public enum ContainerStatus {

    ENABLE(1),
    DISABLE(2);

    private int v;

    public static ContainerStatus of(int v) {
        for (ContainerStatus type : values()) {
            if (type.v == v) {
                return type;
            }
        }
        throw new IllegalArgumentException("unknown ContainerStatus of " + v);
    }
}
