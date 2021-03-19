package tech.powerjob.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Task Processor Type
 *
 * @author tjq
 * @since 2020/3/23
 */
@Getter
@AllArgsConstructor
public enum ProcessorType {

    BUILT_IN(1, "内建处理器"),
    EXTERNAL(4, "外部处理器（动态加载）"),

    @Deprecated
    SHELL(2, "SHELL脚本"),
    @Deprecated
    PYTHON(3, "Python脚本");

    private final int v;
    private final String des;

    public static ProcessorType of(int v) {
        for (ProcessorType type : values()) {
            if (type.v == v) {
                return type;
            }
        }
        throw new IllegalArgumentException("unknown ProcessorType of " + v);
    }
}
