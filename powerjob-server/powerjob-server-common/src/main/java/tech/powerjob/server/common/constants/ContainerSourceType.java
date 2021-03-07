package tech.powerjob.server.common.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 容器类型
 *
 * @author tjq
 * @since 2020/5/15
 */
@Getter
@AllArgsConstructor
public enum ContainerSourceType {

    FatJar(1, "Jar文件"),
    Git(2, "Git代码库");

    private final int v;
    private final String des;

    public static ContainerSourceType of(int v) {
        for (ContainerSourceType type : values()) {
            if (type.v == v) {
                return type;
            }
        }
        throw new IllegalArgumentException("unknown ContainerSourceType of " + v);
    }
}
