package tech.powerjob.server.auth.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.server.auth.*;
import tech.powerjob.server.auth.common.AuthConstants;
import tech.powerjob.common.enums.ErrorCodes;
import tech.powerjob.server.auth.common.PowerJobAuthException;
import tech.powerjob.server.auth.service.WebAuthService;
import tech.powerjob.server.auth.service.permission.PowerJobPermissionService;
import tech.powerjob.server.web.request.ComponentUserRoleInfo;

import javax.annotation.Resource;
import java.util.*;

/**
 * WebAuthService
 *
 * @author tjq
 * @since 2024/2/12
 */
@Slf4j
@Service
public class WebAuthServiceImpl implements WebAuthService {

    @Resource
    private PowerJobPermissionService powerJobPermissionService;


    @Override
    public void grantRole2LoginUser(RoleScope roleScope, Long target, Role role, String extra) {
        Long userId = LoginUserHolder.getUserId();
        if (userId == null) {
            throw new PowerJobAuthException(ErrorCodes.USER_NOT_LOGIN);
        }
        powerJobPermissionService.grantRole(roleScope, target, userId, role, extra);
    }

    @Override
    public void processPermissionOnSave(RoleScope roleScope, Long target, ComponentUserRoleInfo o) {
        ComponentUserRoleInfo componentUserRoleInfo = Optional.ofNullable(o).orElse(new ComponentUserRoleInfo());
        
        Map<Role, Set<Long>> role2Uids = powerJobPermissionService.fetchUserWithPermissions(roleScope, target);
        diffGrant(roleScope, target, Role.OBSERVER, componentUserRoleInfo.getObserver(), role2Uids);
        diffGrant(roleScope, target, Role.QA, componentUserRoleInfo.getQa(), role2Uids);
        diffGrant(roleScope, target, Role.DEVELOPER, componentUserRoleInfo.getDeveloper(), role2Uids);
        diffGrant(roleScope, target, Role.ADMIN, componentUserRoleInfo.getAdmin(), role2Uids);
    }

    @Override
    public ComponentUserRoleInfo fetchComponentUserRoleInfo(RoleScope roleScope, Long target) {
        Map<Role, Set<Long>> role2Uids = powerJobPermissionService.fetchUserWithPermissions(roleScope, target);
        return new ComponentUserRoleInfo()
                .setObserver(Lists.newArrayList(role2Uids.getOrDefault(Role.OBSERVER, Collections.emptySet())))
                .setQa(Lists.newArrayList(role2Uids.getOrDefault(Role.QA, Collections.emptySet())))
                .setDeveloper(Lists.newArrayList(role2Uids.getOrDefault(Role.DEVELOPER, Collections.emptySet())))
                .setAdmin(Lists.newArrayList(role2Uids.getOrDefault(Role.ADMIN, Collections.emptySet())));
    }

    @Override
    public boolean hasPermission(RoleScope roleScope, Long target, Permission permission) {

        PowerJobUser powerJobUser = LoginUserHolder.get();
        if (powerJobUser == null) {
            return false;
        }

        return powerJobPermissionService.hasPermission(powerJobUser.getId(), roleScope, target, permission);
    }

    @Override
    public boolean isGlobalAdmin() {
        return hasPermission(RoleScope.GLOBAL, AuthConstants.GLOBAL_ADMIN_TARGET_ID, Permission.SU);
    }

    @Override
    public Map<Role, List<Long>> fetchMyPermissionTargets(RoleScope roleScope) {

        PowerJobUser powerJobUser = LoginUserHolder.get();
        if (powerJobUser == null) {
            throw new PowerJobAuthException(ErrorCodes.USER_NOT_LOGIN);
        }

        // 展示不考虑穿透权限的问题（即拥有 namespace 权限也可以看到全部的 apps）
        return powerJobPermissionService.fetchUserHadPermissionTargets(roleScope, powerJobUser.getId());
    }

    private void diffGrant(RoleScope roleScope, Long target, Role role, List<Long> uids, Map<Role, Set<Long>> originRole2Uids) {

        Set<Long> originUids = Sets.newHashSet(Optional.ofNullable(originRole2Uids.get(role)).orElse(Collections.emptySet()));
        Set<Long> currentUids = Sets.newHashSet(Optional.ofNullable(uids).orElse(Collections.emptyList()));

        Map<String, Object> extraInfo = Maps.newHashMap();
        extraInfo.put("grantor", LoginUserHolder.getUserName());
        extraInfo.put("source", "diffGrant");
        String extra = JsonUtils.toJSONString(extraInfo);

        Set<Long> allIds = Sets.newHashSet(originUids);
        allIds.addAll(currentUids);

        Set<Long> allIds2 = Sets.newHashSet(allIds);

        // 在 originUids 不在 currentUids，需要取消授权
        allIds.removeAll(currentUids);
        allIds.forEach(cancelPermissionUid -> {
            powerJobPermissionService.retrieveRole(roleScope, target, cancelPermissionUid, role);
            log.info("[WebAuthService] [diffGrant] cancelPermission: roleScope={},target={},uid={},role={}", roleScope, target, cancelPermissionUid, role);
        });

        // 在 currentUids 当不在 orignUids，需要增加授权
        allIds2.removeAll(originUids);
        allIds2.forEach(addPermissionUid -> {
            powerJobPermissionService.grantRole(roleScope, target, addPermissionUid, role, extra);
            log.info("[WebAuthService] [diffGrant] grantPermission: roleScope={},target={},uid={},role={},extra={}", roleScope, target, addPermissionUid, role, extra);
        });
    }
}
