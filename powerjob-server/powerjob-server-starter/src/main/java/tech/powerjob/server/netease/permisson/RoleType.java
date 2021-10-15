package tech.powerjob.server.netease.permisson;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Echo009
 * @since 2021/10/14
 *
 * 极简版本的权限管控
 */
@RequiredArgsConstructor
@Getter
public enum RoleType {

    /**
     * 角色类型
     */
    ADMIN("11111"),
    GENERAL_USER("00010"),
    RD("10111"),
    QA("00011"),
    ;

    private final String permissionFlag;


}
