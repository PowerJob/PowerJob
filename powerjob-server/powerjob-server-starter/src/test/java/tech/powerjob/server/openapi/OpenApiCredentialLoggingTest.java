package tech.powerjob.server.openapi;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import tech.powerjob.client.module.AppAuthRequest;
import tech.powerjob.client.module.AppAuthResult;
import tech.powerjob.common.enums.ErrorCodes;
import tech.powerjob.common.response.PowerResultDTO;
import tech.powerjob.server.openapi.security.OpenApiSecurityService;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OpenApiCredentialLoggingTest {

    @Test
    void unexpectedAuthFailureKeepsLegacyDiagnosticWithoutLoggingSecrets() {
        String canary = "synthetic-openapi-secret-canary";
        AppAuthRequest request = new AppAuthRequest();
        request.setAppName("synthetic-app");
        request.setEncryptedPassword(canary);
        request.setExtra(Collections.singletonMap("secret", canary));
        OpenApiSecurityService security = mock(OpenApiSecurityService.class);
        when(security.authAppByParam(request)).thenThrow(new IllegalStateException(canary));
        OpenAPIController controller = new OpenAPIController(null, null, null, null, null, security, null);
        Logger logger = (Logger) LoggerFactory.getLogger(OpenAPIController.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            PowerResultDTO<AppAuthResult> response = controller.auth(request);
            assertFalse(response.isSuccess());
            assertEquals(ErrorCodes.SYSTEM_UNKNOWN_ERROR.getCode(), response.getCode());
            assertTrue(response.getMessage().contains(canary), "Legacy HTTP error diagnostics remain unchanged");
            assertEquals(1, appender.list.size());
            ILoggingEvent event = appender.list.get(0);
            assertTrue(event.getFormattedMessage().contains("IllegalStateException"));
            assertFalse(event.getFormattedMessage().contains(canary));
            assertNull(event.getThrowableProxy(), "Exception messages may contain credentials");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
