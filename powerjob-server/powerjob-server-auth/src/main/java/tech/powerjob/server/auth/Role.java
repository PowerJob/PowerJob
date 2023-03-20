package tech.powerjob.server.auth;

import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

import static tech.powerjob.server.auth.Permission.*;

/**
 * 角色
 * PowerJob 采用 RBAC 实现权限，出于实际需求的考虑，不决定采用动态权限模型。因此 RBAC 中的角色和权限均在此处定义。
 * 如果有自定义诉求，可以修改 Role 的定义
 *
 * @author tjq
 * @since 2023/3/20
 */
@Getter
@AllArgsConstructor
public enum Role {

    /**
     * 观察者，默认只读权限
     */
    OBSERVER(1, Sets.newHashSet(READ)),
    /**
     * 技术质量，读 + 操作权限
     */
    QA(2, Sets.newHashSet(READ, OPS)),
    /**
     * 开发者，读 + 编辑 + 操作权限
     */
    DEVELOPER(3, Sets.newHashSet(READ, WRITE, OPS)),
    /**
     * 项目的超级管理员
     */
    ADMIN(10, Sets.newHashSet(READ, WRITE, OPS, SU)),
    /**
     * 全局超级管理员
     */
    GOD(100, Sets.newHashSet(READ, WRITE, OPS, SU, GLOBAL_SU))
    ;

    private int v;

    private Set<Permission> permissions;
}
