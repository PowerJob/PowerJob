package tech.powerjob.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types">消息内容类型</a>
 *
 * @author tjq
 * @since 2024/8/10
 */
@Getter
@AllArgsConstructor
public enum MIME {

    APPLICATION_JSON("application/json; charset=utf-8"),

    APPLICATION_FORM("application/x-www-form-urlencoded")
    ;

    private final String code;
}
