package tech.powerjob.worker.common.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 持久化策略
 *
 * @author tjq
 * @since 2020/4/14
 */
@Getter
@AllArgsConstructor
public enum  StoreStrategy {

    DISK("磁盘"),
    MEMORY("内存");

    private final String des;
}
