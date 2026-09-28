package tech.powerjob.server.initializer;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import tech.powerjob.server.auth.PowerJobUser;
import tech.powerjob.server.auth.service.login.PowerJobLoginService;
import tech.powerjob.server.auth.service.permission.PowerJobPermissionService;
import tech.powerjob.server.persistence.remote.model.PwjbUserInfoDO;
import tech.powerjob.server.web.request.ModifyUserInfoRequest;
import tech.powerjob.server.web.service.PwjbUserWebService;

import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BootstrapCredentialLoggingTest {
    @Test
    void configuredPasswordAndLoginTokenAreNotLoggedDuringInitialization() {
        String password = "synthetic-admin-password-canary";
        String jwt = "synthetic-session-token-canary";
        SystemInitializeServiceImpl initializer = new SystemInitializeServiceImpl();
        PwjbUserWebService users = mock(PwjbUserWebService.class);
        PowerJobLoginService login = mock(PowerJobLoginService.class);
        PowerJobPermissionService permissions = mock(PowerJobPermissionService.class);
        when(users.findByUsername("ADMIN")).thenReturn(Optional.empty());
        PwjbUserInfoDO user = new PwjbUserInfoDO();
        user.setId(7L);
        when(users.save(any(ModifyUserInfoRequest.class))).thenReturn(user);
        PowerJobUser loggedIn = new PowerJobUser();
        loggedIn.setId(7L);
        loggedIn.setUsername("ADMIN");
        loggedIn.setJwtToken(jwt);
        when(login.doLogin(any())).thenReturn(loggedIn);
        ReflectionTestUtils.setField(initializer, "defaultAdminPassword", password);
        ReflectionTestUtils.setField(initializer, "pwjbUserWebService", users);
        ReflectionTestUtils.setField(initializer, "powerJobLoginService", login);
        ReflectionTestUtils.setField(initializer, "powerJobPermissionService", permissions);
        Logger logger = (Logger) LoggerFactory.getLogger(SystemInitializeServiceImpl.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            initializer.initAdmin();
            ArgumentCaptor<ModifyUserInfoRequest> request = ArgumentCaptor.forClass(ModifyUserInfoRequest.class);
            verify(users).save(request.capture());
            assertEquals(password, request.getValue().getPassword(), "The configured credential must still initialize the account");
            String messages = appender.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.joining("\n"));
            assertFalse(messages.contains(password));
            assertFalse(messages.contains(jwt));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
