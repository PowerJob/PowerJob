package tech.powerjob.server.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageImpl;
import tech.powerjob.server.auth.*;
import tech.powerjob.server.auth.common.AuthConstants;
import tech.powerjob.server.auth.service.WebAuthService;
import tech.powerjob.server.core.service.AppInfoService;
import tech.powerjob.server.persistence.remote.model.AppInfoDO;
import tech.powerjob.server.persistence.remote.repository.AppInfoRepository;
import tech.powerjob.server.remote.worker.WorkerClusterQueryService;
import tech.powerjob.server.web.controller.AppInfoController;
import tech.powerjob.server.web.request.ComponentUserRoleInfo;
import tech.powerjob.server.web.request.ModifyAppInfoRequest;
import tech.powerjob.server.web.request.QueryAppInfoRequest;
import tech.powerjob.server.web.service.*;
import tech.powerjob.server.web.service.impl.AppWebServiceImpl;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LegacyApplicationContractTest {
    @AfterEach void clearLogin() { LoginUserHolder.clean(); }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void appSaveRetainsLegacyFieldsAndRoleProcessing(boolean update) {
        WebAuthService auth = mock(WebAuthService.class);
        AppInfoService apps = mock(AppInfoService.class);
        NamespaceWebService namespaces = mock(NamespaceWebService.class);
        AppInfoDO existing = new AppInfoDO(); existing.setId(101L); existing.setAppName("fixture");
        when(apps.findById(101L, false)).thenReturn(Optional.of(existing));
        when(apps.save(any())).thenAnswer(i -> { AppInfoDO saved = i.getArgument(0); saved.setId(101L); return saved; });
        AppWebServiceImpl service = new AppWebServiceImpl(auth, apps, mock(AppInfoRepository.class), namespaces, mock(WorkerClusterQueryService.class));
        ModifyAppInfoRequest req = new ModifyAppInfoRequest(); req.setId(update ? 101L : null);
        req.setAppName("fixture"); req.setPassword("synthetic-password"); req.setTitle("title");
        req.setNamespaceId(201L); req.setExtra("{\"allowedBecomeAdminByPassword\":false}");
        ComponentUserRoleInfo roles = new ComponentUserRoleInfo().setQa(Collections.singletonList(7L));
        req.setComponentUserRoleInfo(roles);
        AppInfoDO result = service.save(req);
        assertEquals(req.getPassword(), result.getPassword());
        assertEquals(req.getNamespaceId(), result.getNamespaceId());
        assertEquals(req.getExtra(), result.getExtra());
        verify(auth).processPermissionOnSave(RoleScope.APP, 101L, roles);
        verifyNoMoreInteractions(auth);
        verifyNoInteractions(namespaces);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void listPasswordRetainsReadPermissionContract(boolean canRead) {
        AppWebService apps = mock(AppWebService.class);
        WebAuthService auth = mock(WebAuthService.class);
        AppInfoService passwords = mock(AppInfoService.class);
        AppInfoDO app = new AppInfoDO(); app.setId(101L); app.setNamespaceId(201L);
        app.setGmtCreate(new Date()); app.setGmtModified(new Date());
        when(auth.hasPermission(RoleScope.APP, 101L, Permission.READ)).thenReturn(canRead);
        when(passwords.fetchOriginAppPassword(app)).thenReturn("synthetic-password");
        when(apps.list(any())).thenReturn(new PageImpl<>(Collections.singletonList(app)));
        AppInfoController controller = new AppInfoController(apps, auth, mock(UserWebService.class), passwords, mock(NamespaceWebService.class));
        assertEquals(canRead ? "synthetic-password" : AuthConstants.TIPS_NO_PERMISSION_TO_SEE,
                controller.listAppInfoByQuery(new QueryAppInfoRequest()).getData().getData().get(0).getPassword());
        verify(auth, never()).hasPermission(RoleScope.APP, 101L, Permission.SU);
    }
}
