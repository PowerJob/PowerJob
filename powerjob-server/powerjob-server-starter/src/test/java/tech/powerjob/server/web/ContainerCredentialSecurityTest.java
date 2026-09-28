package tech.powerjob.server.web;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tech.powerjob.common.model.GitRepoInfo;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.server.auth.*;
import tech.powerjob.server.auth.interceptor.PowerJobAuthInterceptor;
import tech.powerjob.server.auth.service.login.PowerJobLoginService;
import tech.powerjob.server.auth.service.impl.WebAuthServiceImpl;
import tech.powerjob.server.auth.service.permission.PowerJobPermissionServiceImpl;
import tech.powerjob.server.core.container.ContainerService;
import tech.powerjob.server.extension.dfs.DFsService;
import tech.powerjob.server.persistence.remote.model.*;
import tech.powerjob.server.persistence.remote.repository.*;
import tech.powerjob.server.remote.worker.WorkerClusterQueryService;
import tech.powerjob.server.web.controller.ContainerController;

import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class ContainerCredentialSecurityTest {
    private static final String SECRET = "synthetic-git-password-canary";
    private ContainerInfoRepository containers;
    private UserRoleRepository roles;
    private ContainerService service;
    private ContainerInfoDO container;
    private MockMvc mvc;

    @BeforeEach
    void setUp() throws Exception {
        containers = mock(ContainerInfoRepository.class);
        roles = mock(UserRoleRepository.class);
        AppInfoRepository apps = mock(AppInfoRepository.class);
        AppInfoDO app = new AppInfoDO(); app.setId(7L); app.setNamespaceId(70L);
        when(apps.findById(7L)).thenReturn(Optional.of(app));
        container = new ContainerInfoDO(); container.setId(101L); container.setAppId(7L);
        container.setContainerName("fixture"); container.setSourceType(2); container.setStatus(1);
        container.setVersion("credential-test-" + UUID.randomUUID());
        GitRepoInfo git = new GitRepoInfo(); git.setRepo("https://example.invalid/synthetic.git");
        git.setBranch("main"); git.setUsername("synthetic-user"); git.setPassword(SECRET);
        container.setSourceInfo(JsonUtils.toJSONString(git));
        when(containers.findById(101L)).thenReturn(Optional.of(container));
        when(containers.findByAppIdAndStatusNot(eq(7L), anyInt())).thenReturn(Collections.singletonList(container));
        service = new ContainerService();
        ReflectionTestUtils.setField(service, "containerInfoRepository", containers);
        WorkerClusterQueryService workers = mock(WorkerClusterQueryService.class);
        when(workers.getAllAliveWorkers(7L)).thenReturn(Collections.emptyList());
        ReflectionTestUtils.setField(service, "workerClusterQueryService", workers);
        DFsService dfs = mock(DFsService.class);
        when(dfs.fetchFileMeta(any())).thenReturn(Optional.empty());
        ReflectionTestUtils.setField(service, "dFsService", dfs);
        PowerJobPermissionServiceImpl permissions = new PowerJobPermissionServiceImpl();
        ReflectionTestUtils.setField(permissions, "appInfoRepository", apps);
        ReflectionTestUtils.setField(permissions, "userRoleRepository", roles);
        PowerJobUser user = new PowerJobUser(); user.setId(9L);
        PowerJobLoginService login = mock(PowerJobLoginService.class);
        when(login.ifLogin(any(javax.servlet.http.HttpServletRequest.class))).thenReturn(Optional.of(user));
        PowerJobAuthInterceptor interceptor = new PowerJobAuthInterceptor();
        ReflectionTestUtils.setField(interceptor, "powerJobLoginService", login);
        ReflectionTestUtils.setField(interceptor, "powerJobPermissionService", permissions);
        ContainerController controller = new ContainerController(service, apps, containers);
        WebAuthServiceImpl webAuth = new WebAuthServiceImpl();
        ReflectionTestUtils.setField(webAuth, "powerJobPermissionService", permissions);
        ReflectionTestUtils.setField(controller, "webAuthService", webAuth);
        mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new ControllerExceptionHandler())
                .addInterceptors(interceptor).build();
        setRole(Role.OBSERVER);
    }

    private void setRole(Role role) {
        UserRoleDO grant = new UserRoleDO(); grant.setUserId(9L); grant.setScope(RoleScope.APP.getV());
        grant.setTarget(7L); grant.setRole(role.getV());
        when(roles.findAllByUserId(9L)).thenReturn(Collections.singletonList(grant));
    }

    private String listSource() throws Exception {
        String response = mvc.perform(get("/container/list").header("AppId", "7"))
                .andExpect(jsonPath("$.success").value(true)).andReturn().getResponse().getContentAsString();
        return JSON.parseObject(response).getJSONArray("data").getJSONObject(0).getString("sourceInfo");
    }

    @AfterEach void cleanLogin() { LoginUserHolder.clean(); }

    @ParameterizedTest
    @ValueSource(strings = {"password", "url-userinfo", "malformed"})
    void observerSeesContainerMetadataWithoutAnyGitSourceCredentials(String variant) throws Exception {
        if (variant.equals("url-userinfo")) container.setSourceInfo("{\"repo\":\"https://user:" + SECRET + "@example.invalid/a.git\"}");
        if (variant.equals("malformed")) container.setSourceInfo("malformed-" + SECRET);
        String original = container.getSourceInfo();
        assertEquals("{}", listSource());
        assertEquals(original, container.getSourceInfo(), "Redaction must not mutate the persistence entity");
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"QA", "DEVELOPER", "ADMIN"})
    void operatorsCanEditMetadataWithoutLosingTheExistingGitPassword(Role role) throws Exception {
        setRole(role);
        String source = listSource();
        assertEquals(SECRET, JsonUtils.parseObject(source, GitRepoInfo.class).getPassword());
        Map<String, Object> request = new HashMap<>();
        request.put("id", 101L); request.put("containerName", "renamed"); request.put("sourceType", "Git");
        request.put("sourceInfo", source); request.put("status", "ENABLE");
        mvc.perform(post("/container/save").header("AppId", "7").contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJSONString(request))).andExpect(jsonPath("$.success").value(true));
        verify(containers).saveAndFlush(argThat(saved -> saved.getContainerName().equals("renamed")
                && SECRET.equals(JSON.parseObject(saved.getSourceInfo(), GitRepoInfo.class).getPassword())));
    }

    @Test
    void observerCannotWriteBackTheHiddenGitConfiguration() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("id", 101L); request.put("containerName", "renamed"); request.put("sourceType", "Git");
        request.put("sourceInfo", listSource()); request.put("status", "ENABLE");
        mvc.perform(post("/container/save").header("AppId", "7").contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJSONString(request))).andExpect(jsonPath("$.success").value(false));
        verify(containers, never()).saveAndFlush(any());
        assertTrue(container.getSourceInfo().contains(SECRET));
    }

    @Test
    void fatJarMetadataRemainsVisibleToReaders() throws Exception {
        container.setSourceType(1); container.setSourceInfo("synthetic-jar-digest");
        assertEquals("synthetic-jar-digest", listSource());
    }

    @ParameterizedTest
    @ValueSource(strings = {"delete", "prepare-failure"})
    void containerLogsAndDeploymentProgressDoNotExposeGitSecrets(String operation) {
        Logger logger = (Logger) LoggerFactory.getLogger(ContainerService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>(); appender.start(); logger.addAppender(appender);
        Session session = mock(Session.class); RemoteEndpoint.Async remote = mock(RemoteEndpoint.Async.class);
        when(session.getAsyncRemote()).thenReturn(remote);
        List<String> progress = new ArrayList<>();
        doAnswer(call -> { progress.add(call.getArgument(0)); return null; }).when(remote).sendText(anyString());
        try {
            if (operation.equals("delete")) service.delete(7L, 101L);
            else {
                // Invalid synthetic JSON fails before JGit is reached: no clone or network operation.
                container.setSourceInfo("not-json-" + SECRET);
                ReflectionTestUtils.invokeMethod(service, "prepareJarFile", container, session);
            }
            String messages = appender.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.joining("\n"));
            assertFalse(messages.contains(SECRET));
            assertFalse(String.join("\n", progress).contains(SECRET));
            assertTrue(appender.list.stream().allMatch(event -> event.getThrowableProxy() == null), "Exceptions may embed credentials");
        } finally { logger.detachAppender(appender); appender.stop(); }
    }
}
