package tech.powerjob.server.auth.service.permission;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;
import tech.powerjob.common.exception.ImpossibleException;
import tech.powerjob.server.auth.Permission;
import tech.powerjob.server.auth.PowerJobUser;
import tech.powerjob.server.auth.Role;
import tech.powerjob.server.auth.RoleScope;
import tech.powerjob.server.auth.interceptor.ApiPermission;
import tech.powerjob.server.auth.interceptor.dp.DynamicPermission;
import tech.powerjob.server.auth.interceptor.dp.EmptyDynamicPermission;
import tech.powerjob.server.persistence.remote.model.AppInfoDO;
import tech.powerjob.server.persistence.remote.model.UserRoleDO;
import tech.powerjob.server.persistence.remote.repository.AppInfoRepository;
import tech.powerjob.server.persistence.remote.repository.UserRoleRepository;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
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
    public boolean hasPermission(HttpServletRequest request, Object handler, PowerJobUser user, ApiPermission apiPermission) {
        final List<UserRoleDO> userRoleList = Optional.ofNullable(userRoleRepository.findAllByUserId(user.getId())).orElse(Collections.emptyList());

        Multimap<Long, Role> appId2Role = ArrayListMultimap.create();
        Multimap<Long, Role> namespaceId2Role = ArrayListMultimap.create();

        List<Role> globalRoles = Lists.newArrayList();

        for (UserRoleDO userRole : userRoleList) {
            final Role role = Role.of(userRole.getRole());
            RoleScope roleScope = RoleScope.of(userRole.getScope());

            // 处理全局权限
            if (RoleScope.GLOBAL.equals(roleScope)) {
                if (Role.ADMIN.equals(role)) {
                    return true;
                }
                globalRoles.add(role);
            }

            if (Objects.equals(userRole.getScope(), RoleScope.NAMESPACE.getV())) {
                namespaceId2Role.put(userRole.getTarget(), role);
            }
            if (Objects.equals(userRole.getScope(), RoleScope.APP.getV())) {
                appId2Role.put(userRole.getTarget(), role);
            }
        }

        // 无超级管理员权限，校验普通权限
        final String appIdStr = request.getHeader("appId");
        if (StringUtils.isEmpty(appIdStr)) {
            throw new IllegalArgumentException("can't find appId in header, please refresh and try again!");
        }

        Long appId = Long.valueOf(appIdStr);

        final Permission requiredPermission = parsePermission(request, handler, apiPermission);
        if (requiredPermission == Permission.NONE) {
            return true;
        }

        final Collection<Role> appRoles = appId2Role.get(appId);
        for (Role role : appRoles) {
            if (role.getPermissions().contains(requiredPermission)) {
                return true;
            }
        }

        // 校验 namespace 穿透权限
        Optional<AppInfoDO> appInfoOpt = appInfoRepository.findById(appId);
        if (!appInfoOpt.isPresent()) {
            throw new IllegalArgumentException("can't find appInfo by appId in permission check: " + appId);
        }
        Long namespaceId = Optional.ofNullable(appInfoOpt.get().getNamespaceId()).orElse(-1L);
        Collection<Role> namespaceRoles = namespaceId2Role.get(namespaceId);
        for (Role role : namespaceRoles) {
            if (role.getPermissions().contains(requiredPermission)) {
                return true;
            }
        }

        // 检验全局穿透权限（按使用频率排列检测顺序，即 app -> namespace -> global）
        for (Role role : globalRoles) {
            if (role.getPermissions().contains(requiredPermission)) {
                return true;
            }
        }

        return false;
    }

    private static Permission parsePermission(HttpServletRequest request, Object handler, ApiPermission apiPermission) {
        Class<? extends DynamicPermission> dynamicPermissionPlugin = apiPermission.dynamicPermissionPlugin();
        if (EmptyDynamicPermission.class.equals(dynamicPermissionPlugin)) {
            return apiPermission.requiredPermission();
        }
        try {
            DynamicPermission dynamicPermission = dynamicPermissionPlugin.getDeclaredConstructor().newInstance();
            return dynamicPermission.calculate(request, handler);
        } catch (Throwable t) {
            log.error("[PowerJobAuthService] process dynamicPermissionPlugin failed!", t);
            ExceptionUtils.rethrow(t);
        }
        throw new ImpossibleException();
    }
}
