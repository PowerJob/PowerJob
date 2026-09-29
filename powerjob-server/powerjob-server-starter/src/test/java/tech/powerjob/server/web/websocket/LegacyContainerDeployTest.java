package tech.powerjob.server.web.websocket;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import tech.powerjob.server.core.container.ContainerService;

import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LegacyContainerDeployTest {
    @ParameterizedTest
    @ValueSource(strings = {"Hello", "{\"jwtToken\":\"synthetic-first-frame-secret\"}"})
    void bothConsoleFramesAreIgnoredDuringDeploymentWithoutLoggingTheirContent(String message) throws Exception {
        ContainerDeployServerEndpoint endpoint = new ContainerDeployServerEndpoint();
        ContainerService containers = mock(ContainerService.class);
        ReflectionTestUtils.setField(endpoint, "containerService", containers);
        Session session = mock(Session.class); RemoteEndpoint.Async remote = mock(RemoteEndpoint.Async.class);
        when(session.getAsyncRemote()).thenReturn(remote);
        when(session.getId()).thenReturn("synthetic-session");
        doAnswer(call -> { endpoint.onMessage(message, session); endpoint.onMessage(message, session); return null; })
                .when(containers).deploy(11L, session);
        Logger logger = (Logger) LoggerFactory.getLogger(ContainerDeployServerEndpoint.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>(); appender.start(); logger.addAppender(appender);
        ch.qos.logback.classic.Level previous = logger.getLevel(); logger.setLevel(ch.qos.logback.classic.Level.DEBUG);
        try {
            endpoint.onOpen(11L, session);
            verify(containers, times(1)).deploy(11L, session);
            verify(session).close();
            assertEquals(2, appender.list.size());
            assertTrue(appender.list.stream().allMatch(event -> event.getFormattedMessage().contains("synthetic-session")));
            assertTrue(appender.list.stream().noneMatch(event -> event.getFormattedMessage().contains(message)));
        } finally { logger.setLevel(previous); logger.detachAppender(appender); appender.stop(); }
    }

    @Test
    void openingLegacyConnectionDeploysAndClosesWithoutAuthenticationFrame() throws Exception {
        ContainerDeployServerEndpoint endpoint = new ContainerDeployServerEndpoint();
        ContainerService containers = mock(ContainerService.class);
        ReflectionTestUtils.setField(endpoint, "containerService", containers);
        Session session = mock(Session.class); RemoteEndpoint.Async remote = mock(RemoteEndpoint.Async.class);
        when(session.getAsyncRemote()).thenReturn(remote);
        endpoint.onOpen(11L, session);
        verify(containers, times(1)).deploy(11L, session);
        verify(remote).sendText("SYSTEM: connected successfully, start to deploy container: 11");
        verify(session).close();
        verify(session, never()).setMaxIdleTimeout(anyLong());
    }

    @Test
    void deploymentFailureKeepsLegacyDiagnosticButServerLogExcludesSecrets() throws Exception {
        ContainerDeployServerEndpoint endpoint = new ContainerDeployServerEndpoint();
        ContainerService containers = mock(ContainerService.class);
        ReflectionTestUtils.setField(endpoint, "containerService", containers);
        Session session = mock(Session.class); RemoteEndpoint.Async remote = mock(RemoteEndpoint.Async.class);
        when(session.getAsyncRemote()).thenReturn(remote);
        String canary = "synthetic-deploy-secret";
        doThrow(new IllegalStateException(canary)).when(containers).deploy(11L, session);
        Logger logger = (Logger) LoggerFactory.getLogger(ContainerDeployServerEndpoint.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>(); appender.start(); logger.addAppender(appender);
        try {
            endpoint.onOpen(11L, session);
            verify(remote).sendText("SYSTEM: deploy failed because of the exception");
            verify(remote).sendText(contains("IllegalStateException: " + canary));
            verify(session).close();
            assertEquals(1, appender.list.size());
            assertFalse(appender.list.get(0).getFormattedMessage().contains(canary));
            assertNull(appender.list.get(0).getThrowableProxy());
        } finally { logger.detachAppender(appender); appender.stop(); }
    }
}
