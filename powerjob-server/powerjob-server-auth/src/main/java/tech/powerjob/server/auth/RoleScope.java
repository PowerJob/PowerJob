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

    /**
     * NAMESPACE 权限
     */
    NAMESPACE(1),
    /**
     * APP 级别权限
     */
    APP(10),
    /**
     * 全局权限
     */
    GLOBAL(666)
    ;

    private final int v;

    public static RoleScope of(int vv) {
        for (RoleScope rs : values()) {
            if (vv == rs.v) {
                return rs;
            }
        }
        throw new IllegalArgumentException("unknown RoleScope: " + vv);
    }
}
