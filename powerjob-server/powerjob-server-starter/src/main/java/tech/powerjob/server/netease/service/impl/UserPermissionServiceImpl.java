package tech.powerjob.server.netease.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tech.powerjob.server.netease.context.NeteaseContext;
import tech.powerjob.server.netease.permisson.PermissionType;
import tech.powerjob.server.netease.permisson.RoleType;
import tech.powerjob.server.netease.service.UserPermissionService;
import tech.powerjob.server.persistence.remote.model.UserInfoDO;
import tech.powerjob.server.persistence.remote.model.UserRoleDO;
import tech.powerjob.server.persistence.remote.repository.AppInfoRepository;
import tech.powerjob.server.persistence.remote.repository.UserRoleRepository;

import java.util.*;

/**
 * @author Echo009
 * @since 2021/10/14
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserPermissionServiceImpl implements UserPermissionService {

    private final UserRoleRepository userRoleRepository;

    private final AppInfoRepository appInfoRepository;

    private static final String SEPARATOR = "#";

    private static final String SUPER_ADMIN_FLAG = "SU";


    @Override
    public boolean hasPermission(String appId, String permissionType) {
        UserInfoDO currentUser = NeteaseContext.getCurrentUser();
        if (currentUser == null) {
            return false;
        }
        Long userId = currentUser.getId();

        List<UserRoleDO> allByUserId = userRoleRepository.findAllByUserId(userId);
        Set<RoleType> roleSet = new HashSet<>();
        for (UserRoleDO userRole : allByUserId) {
            if (userRole.getRole().startsWith(appId) || userRole.getRole().equals(SUPER_ADMIN_FLAG)) {
                roleSet.add(extractRoleType(userRole.getRole()));
            }
        }
        PermissionType targetPermission = PermissionType.valueOf(permissionType);
        int index = targetPermission.getIndex();
        for (RoleType roleType : roleSet) {
            if (roleType == null) {
                continue;
            }
            String permissionFlag = roleType.getPermissionFlag();
            if (permissionFlag.length() > index && permissionFlag.charAt(index) == '1') {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<Long> listAppId() {
        UserInfoDO currentUser = NeteaseContext.getCurrentUser();
        if (currentUser == null) {
            return Collections.emptySet();
        }
        Long userId = currentUser.getId();
        List<UserRoleDO> allByUserId = userRoleRepository.findAllByUserId(userId);
        Set<Long> res = new HashSet<>();
        //检查是否具备管理员权限
        for (UserRoleDO userRole : allByUserId) {
            if (userRole.getRole().startsWith(SUPER_ADMIN_FLAG)) {
                // 返回所有 APP id
                return appInfoRepository.listAllAppId();
            }
            Long appId = extractAppId(userRole.getRole());
            if (appId != null) {
                res.add(appId);
            }
        }
        return res;

    }

    private static Long extractAppId(String role) {
        String[] split = role.split(SEPARATOR);
        try {
            return Long.parseLong(split[0]);
        } catch (Exception ignore) {
            //
        }
        return null;
    }

    private static RoleType extractRoleType(String role) {
        String[] split = role.split(SEPARATOR);
        if (split.length > 1) {
            return RoleType.valueOf(split[1]);
        }
        return null;
    }


}
