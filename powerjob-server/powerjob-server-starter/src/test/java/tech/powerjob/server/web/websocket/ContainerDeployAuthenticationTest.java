package tech.powerjob.server.web.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import tech.powerjob.server.auth.*;
import tech.powerjob.server.auth.service.login.PowerJobLoginService;
import tech.powerjob.server.auth.service.permission.PowerJobPermissionService;
import tech.powerjob.server.core.container.ContainerService;
import tech.powerjob.server.persistence.remote.model.ContainerInfoDO;
import tech.powerjob.server.persistence.remote.repository.ContainerInfoRepository;

import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ContainerDeployAuthenticationTest {
    private ContainerDeployServerEndpoint endpoint;
    private ContainerService containers;
    private ContainerInfoRepository repository;
    private PowerJobLoginService login;
    private PowerJobPermissionService permissions;
    private Session session;
    private RemoteEndpoint.Async remote;

    @BeforeEach
    void setUp() {
        endpoint = new ContainerDeployServerEndpoint();
        containers = mock(ContainerService.class);
        repository = mock(ContainerInfoRepository.class);
        login = mock(PowerJobLoginService.class);
        permissions = mock(PowerJobPermissionService.class);
        session = mock(Session.class);
        remote = mock(RemoteEndpoint.Async.class);
        when(session.getUserProperties()).thenReturn(new HashMap<>());
        when(session.getAsyncRemote()).thenReturn(remote);
        ReflectionTestUtils.setField(endpoint, "containerService", containers);
        ReflectionTestUtils.setField(endpoint, "containerInfoRepository", repository);
        ReflectionTestUtils.setField(endpoint, "powerJobLoginService", login);
        ReflectionTestUtils.setField(endpoint, "permissionService", permissions);
        ContainerInfoDO container = new ContainerInfoDO();
        container.setId(11L);
        container.setAppId(22L);
        when(repository.findById(11L)).thenReturn(Optional.of(container));
        PowerJobUser user = new PowerJobUser();
        user.setId(33L);
        when(login.ifLogin("valid-token")).thenAnswer(invocation -> {
            LoginUserHolder.set(user);
            return Optional.of(user);
        });
    }

    @Test
    void handshakeDoesNotDeploy() {
        endpoint.onOpen(11L, session);
        verifyNoInteractions(containers, login, permissions, repository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"jwtToken\":\"invalid\"}", "not-json", "{\"jwtToken\":123}"})
    void anonymousMalformedOrInvalidFirstFrameCannotDeploy(String message) throws Exception {
        endpoint.onOpen(11L, session);
        endpoint.onMessage(message, session);
        verifyNoInteractions(containers, repository, permissions);
        verify(session).close();
        assertNull(LoginUserHolder.get());
        verify(remote, never()).sendText(contains("invalid"));
    }

    @Test
    void readOnlyUserCannotDeployAndThreadStateIsCleared() throws Exception {
        endpoint.onOpen(11L, session);
        endpoint.onMessage("{\"jwtToken\":\"valid-token\",\"appId\":999}", session);
        verify(permissions).hasPermission(33L, RoleScope.APP, 22L, Permission.OPS);
        verifyNoInteractions(containers);
        verify(session).close();
        assertNull(LoginUserHolder.get());
    }

    @Test
    void authorizedUserDeploysOnceAgainstContainerOwner() throws Exception {
        when(permissions.hasPermission(33L, RoleScope.APP, 22L, Permission.OPS)).thenReturn(true);
        endpoint.onOpen(11L, session);
        endpoint.onMessage("{\"jwtToken\":\"valid-token\",\"appId\":999}", session);
        endpoint.onMessage("{\"jwtToken\":\"valid-token\"}", session);
        verify(containers, times(1)).deploy(11L, session);
        verify(login, times(1)).ifLogin("valid-token");
        assertNull(LoginUserHolder.get());
    }
}
