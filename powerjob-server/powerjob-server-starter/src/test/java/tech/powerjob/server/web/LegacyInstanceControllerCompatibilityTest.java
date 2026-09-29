package tech.powerjob.server.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.model.InstanceDetail;
import tech.powerjob.common.response.InstanceInfoDTO;
import tech.powerjob.server.auth.Permission;
import tech.powerjob.server.auth.PowerJobUser;
import tech.powerjob.server.auth.RoleScope;
import tech.powerjob.server.auth.interceptor.ApiPermission;
import tech.powerjob.server.auth.interceptor.PowerJobAuthInterceptor;
import tech.powerjob.server.auth.service.login.PowerJobLoginService;
import tech.powerjob.server.auth.service.permission.PowerJobPermissionService;
import tech.powerjob.server.core.instance.InstanceLogService;
import tech.powerjob.server.core.instance.InstanceService;
import tech.powerjob.server.web.controller.InstanceController;
import tech.powerjob.server.web.service.LogDownloadTicketService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LegacyInstanceControllerCompatibilityTest {
    private static final long INSTANCE_ID = 9007199254740993L;
    private InstanceController controller;
    private InstanceService instances;
    private InstanceLogService logs;
    @TempDir Path temporary;

    @BeforeEach
    void setUp() {
        controller = new InstanceController();
        instances = mock(InstanceService.class);
        logs = mock(InstanceLogService.class);
        ReflectionTestUtils.setField(controller, "instanceService", instances);
        ReflectionTestUtils.setField(controller, "instanceLogService", logs);
        LogDownloadTicketService tickets = mock(LogDownloadTicketService.class);
        when(tickets.issue(20L, INSTANCE_ID)).thenReturn("compatibility-ticket");
        ReflectionTestUtils.setField(controller, "logDownloadTicketService", tickets);
        InstanceInfoDTO info = new InstanceInfoDTO();
        info.setInstanceId(INSTANCE_ID);
        info.setAppId(20L);
        when(instances.getInstanceInfo(INSTANCE_ID)).thenReturn(info);
        InstanceDetail detail = new InstanceDetail();
        detail.setJobParams("legacy-中文-parameters");
        when(instances.getInstanceDetail(anyLong(), eq(INSTANCE_ID), nullable(String.class))).thenReturn(detail);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"null", "undefined"})
    void oldConsoleWithoutAppHeaderResolvesTheStoredAppBeforeRouting(String header) throws Exception {
        MockHttpServletRequestBuilder request = post("/instance/detailPlus").contentType(MediaType.APPLICATION_JSON)
                .content("{\"instanceId\":" + INSTANCE_ID + "}");
        if (header != null) request.header("AppId", header);
        mvc(controller, false).perform(request)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobParams").value("legacy-中文-parameters"));
        verify(instances).getInstanceInfo(INSTANCE_ID);
        verify(instances).getInstanceDetail(20L, INSTANCE_ID, null);
    }

    @Test
    void olderBodyAppIdAndCurrentHeaderBothRetainRouting() throws Exception {
        MockMvc mvc = mvc(controller, false);
        mvc.perform(post("/instance/detailPlus").contentType(MediaType.APPLICATION_JSON)
                .content("{\"instanceId\":" + INSTANCE_ID + ",\"appId\":21}"))
                .andExpect(jsonPath("$.success").value(true));
        mvc.perform(post("/instance/detailPlus").header("AppId", "22").contentType(MediaType.APPLICATION_JSON)
                .content("{\"instanceId\":" + INSTANCE_ID + ",\"appId\":21}"))
                .andExpect(jsonPath("$.success").value(true));
        verify(instances).getInstanceDetail(21L, INSTANCE_ID, null);
        verify(instances).getInstanceDetail(22L, INSTANCE_ID, null);
        verify(instances, never()).getInstanceInfo(anyLong());
    }

    @Test
    void missingLegacyInstanceReturnsTheExistingDomainErrorInsteadOfNullHeaderFailure() throws Exception {
        when(instances.getInstanceInfo(INSTANCE_ID)).thenThrow(new PowerJobException("instance not found"));
        mvc(controller, false).perform(post("/instance/detailPlus").contentType(MediaType.APPLICATION_JSON)
                .content("{\"instanceId\":" + INSTANCE_ID + "}"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("instance not found")));
        verify(instances, never()).getInstanceDetail(anyLong(), anyLong(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"delete from task", "DROP TABLE task", "UPDATE task SET status=1"})
    void oldDetailContractStillRejectsMutatingQueriesBeforeInstanceLookup(String query) throws Exception {
        mvc(controller, false).perform(post("/instance/detailPlus").contentType(MediaType.APPLICATION_JSON)
                .content("{\"instanceId\":" + INSTANCE_ID + ",\"customQuery\":\"" + query + "\"}"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("illegally query")));
        verifyNoInteractions(instances);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/powerjob", "/nested/server"})
    void newUrlCarriesTheOldOwnerCompatibilityTicketAndNewReceiverAcceptsLegacyBareUrl(String contextPath) throws Exception {
        String url = "http://server-b:7700" + contextPath + "/instance/downloadLog?instanceId=" + INSTANCE_ID;
        when(logs.fetchDownloadUrl(20L, INSTANCE_ID)).thenReturn(url);
        mvc(controller, true).perform(get(contextPath + "/instance/downloadLogUrl").contextPath(contextPath)
                .header("AppId", "20").param("instanceId", Long.toString(INSTANCE_ID)))
                .andExpect(jsonPath("$.data").value(url + "&ticket=compatibility-ticket"));

        // Separate receiving controller: the URL works without the issuing node's JWT/key or local state.
        InstanceController receivingController = new InstanceController();
        InstanceLogService receivingLogs = mock(InstanceLogService.class);
        ReflectionTestUtils.setField(receivingController, "instanceLogService", receivingLogs);
        byte[] expected = "first line\n日志中文😀\n".getBytes(StandardCharsets.UTF_8);
        Path log = temporary.resolve("download.log");
        Files.write(log, expected);
        when(receivingLogs.downloadInstanceLog(INSTANCE_ID)).thenReturn(log.toFile());
        URI target = URI.create(url);
        mvc(receivingController, false).perform(get(target.getPath()).contextPath(contextPath)
                .param("instanceId", Long.toString(INSTANCE_ID)))
                .andExpect(status().isOk()).andExpect(content().bytes(expected))
                .andExpect(header().string("Content-Disposition", "attachment;filename=download.log"));
        verify(receivingLogs).downloadInstanceLog(INSTANCE_ID);
    }

    @Test
    void alreadyIssuedTicketQueryDoesNotInvalidateLegacyDownload() throws Exception {
        Path log = temporary.resolve("existing-url.log");
        byte[] expected = {0, 1, (byte) 0xff};
        Files.write(log, expected);
        when(logs.downloadInstanceLog(INSTANCE_ID)).thenReturn(log.toFile());
        mvc(controller, false).perform(get("/instance/downloadLog").param("instanceId", Long.toString(INSTANCE_ID))
                .param("ticket", "previously-issued-by-5.1.4"))
                .andExpect(content().bytes(expected));
    }

    @Test
    void onlyPreexistingPermissionAnnotationsRemainOnLogRoutes() throws Exception {
        assertNull(InstanceController.class.getMethod("downloadLog4Console", Long.class, HttpServletResponse.class,
                HttpServletRequest.class).getAnnotation(ApiPermission.class));
        ApiPermission urlPermission = InstanceController.class.getMethod("getDownloadUrl", Long.class,
                HttpServletRequest.class).getAnnotation(ApiPermission.class);
        assertEquals(RoleScope.APP, urlPermission.roleScope());
        assertEquals(Permission.READ, urlPermission.requiredPermission());
        ApiPermission logPermission = InstanceController.class.getMethod("getInstanceLog", Long.class, Long.class,
                HttpServletRequest.class).getAnnotation(ApiPermission.class);
        assertEquals(Permission.OPS, logPermission.requiredPermission());
    }

    private static MockMvc mvc(InstanceController controller, boolean loggedIn) {
        PowerJobLoginService login = mock(PowerJobLoginService.class);
        PowerJobPermissionService permissions = mock(PowerJobPermissionService.class);
        PowerJobUser user = new PowerJobUser();
        user.setId(1L);
        when(login.ifLogin(any(javax.servlet.http.HttpServletRequest.class))).thenReturn(loggedIn ? Optional.of(user) : Optional.empty());
        when(permissions.hasPermission(anyLong(), any(), any(), any())).thenReturn(true);
        PowerJobAuthInterceptor interceptor = new PowerJobAuthInterceptor();
        ReflectionTestUtils.setField(interceptor, "powerJobLoginService", login);
        ReflectionTestUtils.setField(interceptor, "powerJobPermissionService", permissions);
        return MockMvcBuilders.standaloneSetup(controller).addInterceptors(interceptor)
                .setControllerAdvice(new ControllerExceptionHandler()).build();
    }
}
