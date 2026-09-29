package tech.powerjob.server.web.service;

import org.apache.commons.collections4.MapUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import tech.powerjob.common.enums.ErrorCodes;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.server.auth.jwt.JwtService;
import tech.powerjob.server.auth.jwt.ParseResult;
import tech.powerjob.server.auth.jwt.impl.DefaultSecretProvider;
import tech.powerjob.server.auth.jwt.impl.JwtServiceImpl;
import tech.powerjob.server.core.service.CacheService;

import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LogDownloadTicketServiceTest {
    private static final long INSTANCE_ID = 9007199254740993L;
    private static final String SHARED_SECRET = "synthetic-shared-secret-0123456789-0123456789";

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void newIssuerIsAcceptedByPublished514And515ValidatorOnAnotherNode(boolean configuredSecret) {
        String secret = configuredSecret ? SHARED_SECRET : null;
        JwtService newNode = jwt("jdbc:mysql://shared/db", secret);
        JwtService oldNode = jwt(configuredSecret ? "jdbc:mysql://another-db-alias/db" : "jdbc:mysql://shared/db", secret);
        String ticket = issuer(newNode).issue(7L, INSTANCE_ID);
        assertDoesNotThrow(() -> validator(oldNode).validate(ticket, INSTANCE_ID));
        // Claims travel in the signature, including the 64-bit ID; no issuing-node cache is shared.
        Map<String, Object> claims = oldNode.parse(ticket, "InstanceLogDownload").getResult();
        assertEquals(INSTANCE_ID, MapUtils.getLong(claims, "instanceId").longValue());
        assertEquals(7L, MapUtils.getLong(claims, "appId").longValue());
    }

    @Test
    void publishedOwnerStillRequiresItsExistingSharedKey() {
        String ticket = issuer(jwt("jdbc:mysql://shared/db", SHARED_SECRET)).issue(7L, INSTANCE_ID);
        Published514Validator oldNode = validator(jwt("jdbc:mysql://shared/db", SHARED_SECRET + "-different"));
        assertThrows(PowerJobException.class, () -> oldNode.validate(ticket, INSTANCE_ID));
    }

    @Test
    void publishedValidatorRejectsBareUrlExplainingWhyCompatibilityIssuanceMustRemain() {
        Published514Validator oldNode = validator(jwt("jdbc:mysql://shared/db", SHARED_SECRET));
        assertThrows(PowerJobException.class, () -> oldNode.validate(null, INSTANCE_ID));
    }

    @Test
    void compatibilityIssuerDoesNotAddAnOwnershipLookupOrPermissionGate() {
        JwtService node = jwt("jdbc:mysql://shared/db", SHARED_SECRET);
        String ticket = issuer(node).issue(99L, INSTANCE_ID);
        assertEquals(99L, MapUtils.getLong(node.parse(ticket, "InstanceLogDownload").getResult(), "appId").longValue());
        // Only the published receiver retains its old validation. New receivers ignore this optional query.
        assertThrows(PowerJobException.class, () -> validator(node).validate(ticket, INSTANCE_ID));
    }

    @Test
    void compatibilityClaimDeadlineAndDomainRemainThePublishedFormat() {
        JwtService node = jwt("jdbc:mysql://shared/db", SHARED_SECRET);
        long before = System.currentTimeMillis();
        String ticket = issuer(node).issue(7L, INSTANCE_ID);
        Map<String, Object> claims = node.parse(ticket, "InstanceLogDownload").getResult();
        assertEquals("InstanceLogDownload", claims.get("scope"));
        long expires = MapUtils.getLong(claims, "downloadExpiresAt");
        assertTrue(expires >= before + 300000);
        assertTrue(expires <= System.currentTimeMillis() + 300000);
        assertThrows(PowerJobException.class, () -> validator(node).validate(ticket, INSTANCE_ID + 1));
    }

    private static LogDownloadTicketService issuer(JwtService jwt) {
        LogDownloadTicketService service = new LogDownloadTicketService();
        ReflectionTestUtils.setField(service, "jwtService", jwt);
        return service;
    }

    private static JwtService jwt(String databaseUrl, String secret) {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.datasource.core.jdbc-url", databaseUrl);
        if (secret != null) environment.withProperty("oms.auth.security.jwt.secret", secret);
        DefaultSecretProvider provider = new DefaultSecretProvider();
        ReflectionTestUtils.setField(provider, "environment", environment);
        JwtServiceImpl service = new JwtServiceImpl();
        ReflectionTestUtils.setField(service, "secretProvider", provider);
        ReflectionTestUtils.setField(service, "jwtExpireTime", 600);
        return service;
    }

    private static Published514Validator validator(JwtService jwt) {
        Published514Validator validator = new Published514Validator();
        validator.jwtService = jwt;
        validator.cacheService = mock(CacheService.class);
        when(validator.cacheService.getAppIdByInstanceId(INSTANCE_ID)).thenReturn(7L);
        return validator;
    }

    /**
     * Frozen validate/checkOwner/invalidTicket bodies from v5.1.4 df78b6dd
     * (unchanged in v5.1.5 b35894ef). This is a wire-compatibility oracle, not new-node policy.
     */
    private static class Published514Validator {
        private static final String SIGNING_SCOPE = "InstanceLogDownload";
        private JwtService jwtService;
        private CacheService cacheService;

        public void checkOwner(Long appId, Long instanceId) {
            if (appId == null || instanceId == null || !Objects.equals(appId, cacheService.getAppIdByInstanceId(instanceId))) {
                throw invalidTicket();
            }
        }

        public void validate(String ticket, Long instanceId) {
            if (ticket == null || ticket.isEmpty()) {
                throw invalidTicket();
            }
            ParseResult parsed = jwtService.parse(ticket, SIGNING_SCOPE);
            if (parsed.getStatus() != ParseResult.Status.SUCCESS || parsed.getResult() == null) {
                throw invalidTicket();
            }
            Map<String, Object> claims = parsed.getResult();
            Long expires = MapUtils.getLong(claims, "downloadExpiresAt");
            if (!SIGNING_SCOPE.equals(claims.get("scope")) || expires == null || expires <= System.currentTimeMillis()
                    || !Objects.equals(instanceId, MapUtils.getLong(claims, "instanceId"))) {
                throw invalidTicket();
            }
            checkOwner(MapUtils.getLong(claims, "appId"), instanceId);
        }

        private PowerJobException invalidTicket() {
            return new PowerJobException(ErrorCodes.INVALID_REQUEST, "Invalid log download authorization");
        }
    }
}
