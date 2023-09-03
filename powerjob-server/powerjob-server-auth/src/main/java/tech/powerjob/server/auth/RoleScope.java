package tech.powerjob.server.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 权限范围
 *
 * @author tjq
 * @since 2023/9/3
 */
@Getter
@AllArgsConstructor
public enum RoleScope {

    NAMESPACE(1),

    APP(10)
    ;

    private final int v;
}
