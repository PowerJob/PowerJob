package tech.powerjob.server.auth.service.permission;

import com.google.common.collect.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tech.powerjob.server.auth.Permission;
import tech.powerjob.server.auth.Role;
import tech.powerjob.server.auth.RoleScope;
import tech.powerjob.server.persistence.remote.model.AppInfoDO;
import tech.powerjob.server.persistence.remote.model.UserRoleDO;
import tech.powerjob.server.persistence.remote.repository.AppInfoRepository;
import tech.powerjob.server.persistence.remote.repository.UserRoleRepository;

import javax.annotation.Resource;
import java.util.*;

/**
 * PowerJobPermissionService
 *
 * @author tjq
 * @since 2024/2/11
 */
@Slf4j
@Service
public class PowerJobPermissionServiceImpl implements PowerJobPermissionService {

    @Resource
    private AppInfoRepository appInfoRepository;
    @Resource
    private UserRoleRepository userRoleRepository;

    @Override
    public boolean hasPermission(Long userId, RoleScope roleScope, Long target, Permission requiredPermission) {
        final List<UserRoleDO> userRoleList = Optional.ofNullable(userRoleRepository.findAllByUserId(userId)).orElse(Collections.emptyList());

        Multimap<Long, Role> appId2Role = ArrayListMultimap.create();
        Multimap<Long, Role> namespaceId2Role = ArrayListMultimap.create();

        List<Role> globalRoles = Lists.newArrayList();

        for (UserRoleDO userRole : userRoleList) {
            final Role role = Role.of(userRole.getRole());

            // 处理全局权限
            if (RoleScope.GLOBAL.getV() == userRole.getScope()) {
                if (Role.ADMIN.equals(role)) {
                    return true;
                }
                globalRoles.add(role);
            }

            if (RoleScope.NAMESPACE.getV() == userRole.getScope()) {
                namespaceId2Role.put(userRole.getTarget(), role);
            }
            if (RoleScope.APP.getV() == userRole.getScope()) {
                appId2Role.put(userRole.getTarget(), role);
            }
        }

        // 前置判断需要的权限（新增场景还没有 appId or namespaceId）
        if (requiredPermission == Permission.NONE) {
            return true;
        }

        // 检验全局穿透权限
        for (Role role : globalRoles) {
            if (role.getPermissions().contains(requiredPermission)) {
                return true;
            }
        }

        // 无超级管理员权限，校验普通权限
        if (RoleScope.APP.equals(roleScope)) {
            return checkAppPermission(target, requiredPermission, appId2Role, namespaceId2Role);
        }

        if (RoleScope.NAMESPACE.equals(roleScope)) {
            return checkNamespacePermission(target, requiredPermission, namespaceId2Role);
        }

        return false;
    }

    @Override
    public void grantRole(RoleScope roleScope, Long target, Long userId, Role role, String extra) {

        UserRoleDO userRoleDO = new UserRoleDO();
        userRoleDO.setGmtCreate(new Date());
        userRoleDO.setGmtModified(new Date());
        userRoleDO.setExtra(extra);

        userRoleDO.setScope(roleScope.getV());
        userRoleDO.setTarget(target);
        userRoleDO.setUserId(userId);
        userRoleDO.setRole(role.getV());

        userRoleRepository.saveAndFlush(userRoleDO);
        log.info("[PowerJobPermissionService] [grantPermission] saveAndFlush userRole successfully: {}", userRoleDO);
    }

    @Override
    public void retrieveRole(RoleScope roleScope, Long target, Long userId, Role role) {
        List<UserRoleDO> originUserRole = userRoleRepository.findAllByScopeAndTargetAndRoleAndUserId(roleScope.getV(), target, role.getV(), userId);
        log.info("[PowerJobPermissionService] [retrievePermission] origin rule: {}", originUserRole);
        Optional.ofNullable(originUserRole).orElse(Collections.emptyList()).forEach(r -> {
            userRoleRepository.deleteById(r.getId());
            log.info("[PowerJobPermissionService] [retrievePermission] delete UserRole: {}", r);
        });
    }

    @Override
    public Map<Role, Set<Long>> fetchUserWithPermissions(RoleScope roleScope, Long target) {
        List<UserRoleDO> permissionUserList = userRoleRepository.findAllByScopeAndTarget(roleScope.getV(), target);
        Map<Role, Set<Long>> ret = Maps.newHashMap();
        Optional.ofNullable(permissionUserList).orElse(Collections.emptyList()).forEach(userRoleDO -> {
            Role role = Role.of(userRoleDO.getRole());
            Set<Long> userIds = ret.computeIfAbsent(role, ignore -> Sets.newHashSet());
            userIds.add(userRoleDO.getUserId());
        });

        return ret;
    }

    @Override
    public Map<Role, List<Long>> fetchUserHadPermissionTargets(RoleScope roleScope, Long userId) {

        Map<Role, List<Long>> ret = Maps.newHashMap();
        List<UserRoleDO> userRoleDOList = userRoleRepository.findAllByUserIdAndScope(userId, roleScope.getV());

        Optional.ofNullable(userRoleDOList).orElse(Collections.emptyList()).forEach(r -> {
            Role role = Role.of(r.getRole());
            List<Long> targetIds = ret.computeIfAbsent(role, ignore -> Lists.newArrayList());
            targetIds.add(r.getTarget());
        });

        return ret;
    }

    private boolean checkAppPermission(Long targetId, Permission requiredPermission, Multimap<Long, Role> appId2Role, Multimap<Long, Role> namespaceId2Role) {

        final Collection<Role> appRoles = appId2Role.get(targetId);
        for (Role role : appRoles) {
            if (role.getPermissions().contains(requiredPermission)) {
                return true;
            }
        }

        // 校验 namespace 穿透权限
        Optional<AppInfoDO> appInfoOpt = appInfoRepository.findById(targetId);
        if (!appInfoOpt.isPresent()) {
            throw new IllegalArgumentException("can't find appInfo by appId in permission check: " + targetId);
        }
        Long namespaceId = Optional.ofNullable(appInfoOpt.get().getNamespaceId()).orElse(-1L);
        Collection<Role> namespaceRoles = namespaceId2Role.get(namespaceId);
        for (Role role : namespaceRoles) {
            if (role.getPermissions().contains(requiredPermission)) {
                return true;
            }
        }

        return false;
    }

    private boolean checkNamespacePermission(Long targetId, Permission requiredPermission, Multimap<Long, Role> namespaceId2Role) {
        Collection<Role> namespaceRoles = namespaceId2Role.get(targetId);
        for (Role role : namespaceRoles) {
            if (role.getPermissions().contains(requiredPermission)) {
                return true;
            }
        }

        return false;
    }

}
