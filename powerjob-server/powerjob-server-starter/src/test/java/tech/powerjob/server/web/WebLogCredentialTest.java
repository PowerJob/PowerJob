package tech.powerjob.server.web;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebLogCredentialTest {
    @ParameterizedTest
    @ValueSource(strings = {"originParams", "password", "gitSecret"})
    void accessLogRetainsRequestMetadataWithoutSerializingCredentials(String field) {
        String canary = "synthetic-access-log-secret-canary";
        JoinPoint point = mock(JoinPoint.class);
        Signature signature = mock(Signature.class);
        when(point.getSignature()).thenReturn(signature);
        when(signature.getDeclaringType()).thenReturn(WebLogCredentialTest.class);
        when(signature.getName()).thenReturn("syntheticLogin");
        when(point.getArgs()).thenReturn(new Object[]{Collections.singletonMap(field, canary)});
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/synthetic-login");
        request.setRemoteAddr("127.0.0.1");
        request.setQueryString("token=" + canary);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        Logger logger = (Logger) LoggerFactory.getLogger("WEB_LOG");
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            new WebLogAspect().doBefore(point);
            String messages = appender.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.joining("\n"));
            assertTrue(messages.contains("POST"));
            assertTrue(messages.contains("syntheticLogin"));
            assertFalse(messages.contains(canary));
        } finally {
            RequestContextHolder.resetRequestAttributes();
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
