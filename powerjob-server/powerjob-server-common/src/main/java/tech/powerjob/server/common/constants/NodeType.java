package tech.powerjob.server.common.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 节点类型
 *
 * @author Echo009
 * @since 2021/3/7
 */
@Getter
@AllArgsConstructor
public enum NodeType {
    /**
     * 普通节点
     */
    COMMON(1);


    private final int code;

}
