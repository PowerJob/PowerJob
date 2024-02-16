package tech.powerjob.server.auth.service.permission;

import tech.powerjob.server.auth.Permission;
import tech.powerjob.server.auth.Role;
import tech.powerjob.server.auth.RoleScope;

import java.util.List;
import java.util.Map;

/**
 * PowerJob 鉴权服务
 *
 * @author tjq
 * @since 2024/2/11
 */
public interface PowerJobPermissionService {


    /**
     * 判断用户是否有访问权限
     * @param userId userId
     * @param roleScope 权限范围
     * @param target 权限目标ID
     * @param permission 要求的权限
     * @return 是否有权限
     */
    boolean hasPermission(Long userId, RoleScope roleScope, Long target, Permission permission);

    /**
     * 授予用户角色
     * @param roleScope 权限范围
     * @param target 权限目标
     * @param userId 用户ID
     * @param role 角色
     * @param extra 其他
     */
    void grantRole(RoleScope roleScope, Long target, Long userId, Role role, String extra);

    /**
     * 回收用户角色
     * @param roleScope 权限范围
     * @param target 权限目标
     * @param userId 用户ID
     * @param role 角色
     */
    void retrieveRole(RoleScope roleScope, Long target, Long userId, Role role);

    /**
     * 获取有相关权限的用户
     * @param roleScope 角色范围
     * @param target 目标
     * @return 角色对应的用户列表
     */
    Map<Role, List<Long>> fetchUserWithPermissions(RoleScope roleScope, Long target);

    /**
     * 获取用户有权限的目标
     * @param roleScope 角色范围
     * @param userId 用户ID
     * @return result
     */
    Map<Role, List<Long>> fetchUserHadPermissionTargets(RoleScope roleScope, Long userId);
}
