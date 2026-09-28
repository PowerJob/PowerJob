package tech.powerjob.server.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.server.auth.*;
import tech.powerjob.server.auth.common.AuthConstants;
import tech.powerjob.server.auth.service.WebAuthService;
import tech.powerjob.server.auth.service.impl.WebAuthServiceImpl;
import tech.powerjob.server.auth.service.permission.PowerJobPermissionService;
import tech.powerjob.server.core.service.AppInfoService;
import tech.powerjob.server.persistence.remote.model.*;
import tech.powerjob.server.persistence.remote.repository.*;
import tech.powerjob.server.remote.worker.WorkerClusterQueryService;
import tech.powerjob.server.web.controller.AppInfoController;
import tech.powerjob.server.web.request.*;
import tech.powerjob.server.web.service.*;
import tech.powerjob.server.web.service.impl.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ApplicationPrivilegeTest {
    @AfterEach void clearLogin() { LoginUserHolder.clean(); }

    @ParameterizedTest
    @CsvSource({"APP,false", "APP,true", "NAMESPACE,false", "NAMESPACE,true"})
    void onlyAdministratorsMayChangeAnExistingRoleSet(RoleScope scope, boolean administrator) {
        WebAuthServiceImpl service = new WebAuthServiceImpl();
        PowerJobPermissionService permissions = mock(PowerJobPermissionService.class);
        ReflectionTestUtils.setField(service, "powerJobPermissionService", permissions);
        PowerJobUser user = new PowerJobUser(); user.setId(7L); LoginUserHolder.set(user);
        when(permissions.hasPermission(7L, scope, 101L, Permission.SU)).thenReturn(administrator);
        Map<Role, Set<Long>> roles = new EnumMap<>(Role.class);
        roles.put(Role.ADMIN, Collections.singleton(1L));
        roles.put(Role.DEVELOPER, Collections.singleton(7L));
        when(permissions.fetchUserWithPermissions(scope, 101L)).thenReturn(roles);
        ComponentUserRoleInfo unchanged = new ComponentUserRoleInfo().setAdmin(Collections.singletonList(1L)).setDeveloper(Collections.singletonList(7L));
        assertDoesNotThrow(() -> service.checkPermissionChange(scope, 101L, unchanged));
        ComponentUserRoleInfo escalation = new ComponentUserRoleInfo().setAdmin(Arrays.asList(1L, 7L)).setDeveloper(Collections.singletonList(7L));
        if (administrator) assertDoesNotThrow(() -> service.checkPermissionChange(scope, 101L, escalation));
        else assertThrows(PowerJobException.class, () -> service.checkPermissionChange(scope, 101L, escalation));
        verify(permissions, never()).grantRole(any(), anyLong(), anyLong(), any(), anyString());
    }

    private AppInfoDO app() {
        AppInfoDO app = new AppInfoDO(); app.setId(101L); app.setNamespaceId(201L); app.setAppName("fixture");
        app.setPassword("stored-password"); app.setGmtCreate(new Date()); app.setGmtModified(new Date());
        return app;
    }

    private ModifyAppInfoRequest request() {
        ModifyAppInfoRequest req = new ModifyAppInfoRequest(); req.setId(101L); req.setNamespaceId(201L);
        req.setAppName("fixture"); req.setPassword(AuthConstants.TIPS_NO_PERMISSION_TO_SEE); req.setTitle("updated title");
        return req;
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void changingPasswordRequiresAdministratorWhileMaskedMetadataEditsWork(boolean administrator) {
        WebAuthService auth = mock(WebAuthService.class);
        AppInfoService apps = mock(AppInfoService.class);
        NamespaceWebService namespaces = mock(NamespaceWebService.class);
        AppInfoDO app = app();
        when(namespaces.findById(201L)).thenReturn(Optional.of(new NamespaceDO()));
        when(apps.findById(101L, false)).thenReturn(Optional.of(app));
        when(apps.fetchOriginAppPassword(app)).thenReturn("original-password");
        when(apps.save(any())).thenAnswer(i -> i.getArgument(0));
        when(auth.hasPermission(RoleScope.APP, 101L, Permission.SU)).thenReturn(administrator);
        AppWebServiceImpl service = new AppWebServiceImpl(auth, apps, mock(AppInfoRepository.class), namespaces, mock(WorkerClusterQueryService.class));
        ModifyAppInfoRequest req = request();
        assertEquals("original-password", service.save(req).getPassword());
        assertEquals("updated title", app.getTitle());
        req.setPassword("replacement-password");
        if (administrator) assertEquals("replacement-password", service.save(req).getPassword());
        else { assertThrows(PowerJobException.class, () -> service.save(req)); assertEquals("original-password", app.getPassword()); }
        verify(auth, atLeastOnce()).checkPermissionChange(RoleScope.APP, 101L, null);
    }

    @Test
    void applicationCannotBeCreatedInMissingNamespace() {
        AppInfoService apps = mock(AppInfoService.class);
        AppWebServiceImpl service = new AppWebServiceImpl(mock(WebAuthService.class), apps, mock(AppInfoRepository.class), mock(NamespaceWebService.class), mock(WorkerClusterQueryService.class));
        ModifyAppInfoRequest req = request(); req.setId(null);
        assertThrows(IllegalArgumentException.class, () -> service.save(req));
        verify(apps, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void creationRequiresTargetNamespaceWritePermission(boolean allowed) {
        WebAuthService auth = mock(WebAuthService.class);
        AppInfoService apps = mock(AppInfoService.class);
        NamespaceWebService namespaces = mock(NamespaceWebService.class);
        when(namespaces.findById(201L)).thenReturn(Optional.of(new NamespaceDO()));
        when(auth.hasPermission(RoleScope.NAMESPACE, 201L, Permission.WRITE)).thenReturn(allowed);
        when(apps.save(any())).thenAnswer(i -> { AppInfoDO value = i.getArgument(0); value.setId(101L); return value; });
        AppWebServiceImpl service = new AppWebServiceImpl(auth, apps, mock(AppInfoRepository.class), namespaces, mock(WorkerClusterQueryService.class));
        ModifyAppInfoRequest req = request(); req.setId(null); req.setPassword("new-password");
        if (allowed) assertEquals(201L, service.save(req).getNamespaceId());
        else { assertThrows(PowerJobException.class, () -> service.save(req)); verify(apps, never()).save(any()); }
    }

    @Test
    void namespaceMoveRequiresPermissionOnTargetEvenForApplicationAdministrator() {
        WebAuthService auth = mock(WebAuthService.class);
        AppInfoService apps = mock(AppInfoService.class);
        NamespaceWebService namespaces = mock(NamespaceWebService.class);
        AppInfoDO app = app();
        when(namespaces.findById(202L)).thenReturn(Optional.of(new NamespaceDO()));
        when(apps.findById(101L, false)).thenReturn(Optional.of(app));
        when(apps.fetchOriginAppPassword(app)).thenReturn("original-password");
        when(auth.hasPermission(RoleScope.APP, 101L, Permission.SU)).thenReturn(true);
        AppWebServiceImpl service = new AppWebServiceImpl(auth, apps, mock(AppInfoRepository.class), namespaces, mock(WorkerClusterQueryService.class));
        ModifyAppInfoRequest req = request(); req.setNamespaceId(202L);
        assertThrows(PowerJobException.class, () -> service.save(req));
        assertEquals(201L, app.getNamespaceId()); verify(apps, never()).save(any());
        when(auth.hasPermission(RoleScope.NAMESPACE, 202L, Permission.WRITE)).thenReturn(true);
        when(apps.save(any())).thenAnswer(i -> i.getArgument(0));
        assertEquals(202L, service.save(req).getNamespaceId());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void listAndSaveResponsesOnlyRevealPasswordToAdministrators(boolean administrator) {
        AppWebService apps = mock(AppWebService.class);
        WebAuthService auth = mock(WebAuthService.class);
        AppInfoService passwords = mock(AppInfoService.class);
        AppInfoDO app = app();
        when(auth.hasPermission(RoleScope.APP, 101L, Permission.SU)).thenReturn(administrator);
        when(passwords.fetchOriginAppPassword(app)).thenReturn("original-password");
        when(apps.list(any())).thenReturn(new PageImpl<>(Collections.singletonList(app)));
        when(apps.save(any())).thenReturn(app);
        AppInfoController controller = new AppInfoController(apps, auth, mock(UserWebService.class), passwords, mock(NamespaceWebService.class));
        String expected = administrator ? "original-password" : AuthConstants.TIPS_NO_PERMISSION_TO_SEE;
        assertEquals(expected, controller.listAppInfoByQuery(new QueryAppInfoRequest()).getData().getData().get(0).getPassword());
        assertEquals(expected, controller.saveAppInfo(new ModifyAppInfoRequest(), new MockHttpServletRequest()).getData().getPassword());
        if (!administrator) verify(passwords, never()).fetchOriginAppPassword(any());
    }
    @ParameterizedTest
    @CsvSource({"false,removed", "false,empty", "false,enabled", "false,preserved", "true,removed", "true,empty", "true,enabled", "true,preserved"})
    void passwordBasedAdminGrantPolicyRequiresAdministrator(boolean administrator, String variant) {
        WebAuthService auth = mock(WebAuthService.class);
        AppInfoService apps = mock(AppInfoService.class);
        NamespaceWebService namespaces = mock(NamespaceWebService.class);
        AppInfoDO app = app(); app.setExtra("{\"allowedBecomeAdminByPassword\":false}");
        when(namespaces.findById(201L)).thenReturn(Optional.of(new NamespaceDO()));
        when(apps.findById(101L, false)).thenReturn(Optional.of(app));
        when(apps.fetchOriginAppPassword(app)).thenReturn("original-password");
        when(auth.hasPermission(RoleScope.APP, 101L, Permission.SU)).thenReturn(administrator);
        when(apps.save(any())).thenAnswer(i -> i.getArgument(0));
        AppWebServiceImpl service = new AppWebServiceImpl(auth, apps, mock(AppInfoRepository.class), namespaces, mock(WorkerClusterQueryService.class));
        ModifyAppInfoRequest req = request();
        String extra = "removed".equals(variant) ? null : "empty".equals(variant) ? "{}" : "enabled".equals(variant) ? "{\"allowedBecomeAdminByPassword\":true}" : "{\"allowedBecomeAdminByPassword\":false,\"description\":\"edited\"}";
        req.setExtra(extra);
        if (administrator || "preserved".equals(variant)) {
            assertEquals(extra, service.save(req).getExtra());
            assertEquals("original-password", app.getPassword());
        } else {
            assertThrows(PowerJobException.class, () -> service.save(req));
            assertEquals("{\"allowedBecomeAdminByPassword\":false}", app.getExtra());
            verify(apps, never()).save(any());
        }
    }

}
