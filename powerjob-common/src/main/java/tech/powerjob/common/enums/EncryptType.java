package tech.powerjob.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 加密类型
 *
 * @author tjq
 * @since 2024/8/10
 */
@Getter
@AllArgsConstructor
public enum EncryptType {

    NONE("none"),

    MD5("md5")
    ;

    private final String code;
}
