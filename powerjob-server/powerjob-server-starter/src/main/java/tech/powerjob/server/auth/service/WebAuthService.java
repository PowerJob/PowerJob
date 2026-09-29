package tech.powerjob.server.auth.service;

import tech.powerjob.server.auth.Permission;
import tech.powerjob.server.auth.Role;
import tech.powerjob.server.auth.RoleScope;
import tech.powerjob.server.web.request.ComponentUserRoleInfo;

import java.util.List;
import java.util.Map;

/**
 * Web Auth 服务
 * 写在 starter 包下，抽取 controller 的公共逻辑
 * （powerjob 的 service/core 包核心处理调度核心逻辑，admin 部分代码收口在 stater 包）
 *
 * @author tjq
 * @since 2024/2/12
 */
public interface WebAuthService {

    /**
     * 对当前登录用户授予角色
     * @param roleScope 角色范围
     * @param target 目标
     * @param role 角色
     * @param extra 其他信息
     */
    void grantRole2LoginUser(RoleScope roleScope, Long target, Role role, String extra);

    /**
     * 处理授权
     * @param roleScope 权限范围
     * @param target 权限目标
     * @param componentUserRoleInfo 人员角色信息
     */
    void processPermissionOnSave(RoleScope roleScope, Long target, ComponentUserRoleInfo componentUserRoleInfo);

    /**
     * 获取目标相关权限人员列表
     * @param roleScope 权限范围
     * @param target 权限目标
     * @return ComponentUserRoleInfo
     */
    ComponentUserRoleInfo fetchComponentUserRoleInfo(RoleScope roleScope, Long target);

    /**
     * 判断当前用户是否有权限
     * @param roleScope 权限范围
     * @param target 权限目标
     * @param permission 要求的权限
     * @return 是否有权限
     */
    boolean hasPermission(RoleScope roleScope, Long target, Permission permission);

    /**
     * 是否为全局管理员
     * @return true or false
     */
    boolean isGlobalAdmin();

    Map<Role, List<Long>> fetchMyPermissionTargets(RoleScope roleScope);
}
