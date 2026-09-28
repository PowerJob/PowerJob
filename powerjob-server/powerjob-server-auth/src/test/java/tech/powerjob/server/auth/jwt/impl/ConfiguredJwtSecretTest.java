package tech.powerjob.server.auth.jwt.impl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import tech.powerjob.common.utils.DigestUtils;
import tech.powerjob.server.auth.jwt.ParseResult;

import java.util.Collections;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ConfiguredJwtSecretTest {
    private static final String SECRET_A = "synthetic-key-a-0123456789-0123456789";
    private static final String SECRET_B = "synthetic-key-b-0123456789-0123456789";

    @Test
    void sameSecretValidatesAcrossServersWithDifferentDatabaseUrls() {
        String token = service("jdbc:mysql://node-a/db", SECRET_A).build(Collections.singletonMap("uid", 42L), null);
        assertEquals(ParseResult.Status.SUCCESS, service("jdbc:mysql://node-b/db", SECRET_A).parse(token, null).getStatus());
    }

    @Test
    void changingConfiguredSecretRevokesTokensEvenWhenDatabaseIsUnchanged() {
        String token = service("jdbc:mysql://same/db", SECRET_A).build(Collections.singletonMap("uid", 42L), null);
        assertEquals(ParseResult.Status.FAILED, service("jdbc:mysql://same/db", SECRET_B).parse(token, null).getStatus());
    }

    @Test
    void configuredSecretCannotBeReplacedByPublicDatabaseDerivedKey() {
        String forged = JwtServiceImpl.innerBuild(DigestUtils.md5("jdbc:mysql://same/db"), 60, Collections.singletonMap("uid", 42L));
        assertEquals(ParseResult.Status.FAILED, service("jdbc:mysql://same/db", SECRET_A).parse(forged, null).getStatus());
    }

    @Test
    void arbitraryUtf8SecretWorksWithoutBeingInterpretedAsBase64() {
        JwtServiceImpl service = service("jdbc:mysql://same/db", "synthetic-secret-with-special-characters-!@#中文");
        String jwt = service.build(Collections.singletonMap("uid", 42L), "extra");
        assertEquals(ParseResult.Status.SUCCESS, service.parse(jwt, "extra").getStatus());
        assertEquals(ParseResult.Status.FAILED, service.parse(jwt, "other").getStatus());
    }

    @Test
    void explicitlyWeakOrEmptySecretsFailClosed() {
        for (String secret : new String[]{"", " ", "short"}) {
            assertThrows(IllegalArgumentException.class, () -> provider("jdbc:mysql://db/db", secret).fetchSecretKey());
        }
    }

    @Test
    void absentConfigurationPreservesExistingTokenCompatibility() {
        assertEquals(DigestUtils.md5("jdbc:mysql://legacy/db"), provider("jdbc:mysql://legacy/db", null).fetchSecretKey());
        String jwt = JwtServiceImpl.innerBuild(DigestUtils.md5("jdbc:mysql://legacy/db"), 60, Collections.singletonMap("uid", 42L));
        assertEquals(ParseResult.Status.SUCCESS, service("jdbc:mysql://legacy/db", null).parse(jwt, null).getStatus());
    }

    @Test
    void rejectedTokenAndExtraSecretAreNotLoggedOrEchoed() {
        String jwt = "canary-malformed-jwt";
        String extra = "canary-extra-secret";
        Logger logger = (Logger) LoggerFactory.getLogger(JwtServiceImpl.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            ParseResult result = service("jdbc:mysql://db/db", SECRET_A).parse(jwt, extra);
            assertEquals(ParseResult.Status.FAILED, result.getStatus());
            String logs = appender.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.joining("\n"));
            assertFalse(logs.contains(jwt));
            assertFalse(logs.contains(extra));
            assertTrue(appender.list.stream().allMatch(event -> event.getThrowableProxy() == null), "Exception details may contain rejected token content");
            assertFalse(result.getMsg().contains(jwt));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void databaseUrlAndDerivedKeyDoNotAppearAtDebugLevel() {
        String url = "jdbc:mysql://localhost/db?password=synthetic-canary";
        Logger logger = (Logger) LoggerFactory.getLogger(DefaultSecretProvider.class);
        Level previous = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);
        try {
            String derived = provider(url, null).fetchSecretKey();
            String logs = appender.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.joining("\n"));
            assertFalse(logs.contains(url));
            assertFalse(logs.contains(derived));
        } finally {
            logger.setLevel(previous);
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private static DefaultSecretProvider provider(String url, String secret) {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.datasource.core.jdbc-url", url);
        if (secret != null) environment.withProperty("oms.auth.security.jwt.secret", secret);
        DefaultSecretProvider provider = new DefaultSecretProvider();
        ReflectionTestUtils.setField(provider, "environment", environment);
        return provider;
    }

    private static JwtServiceImpl service(String url, String secret) {
        JwtServiceImpl service = new JwtServiceImpl();
        ReflectionTestUtils.setField(service, "secretProvider", provider(url, secret));
        ReflectionTestUtils.setField(service, "jwtExpireTime", 60);
        return service;
    }
}
