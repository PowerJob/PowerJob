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
public enum PermissionType  {
    /**
     * 权限类型
     *
     * 增删改查、运维
     */
    CREATE(1),
    DELETE(2),
    UPDATE(3),
    QUERY(4),
    OPERATING(5),
    ;
    /**
     * 偏移，从左边第一位开始
     */
    private final int index;


}
