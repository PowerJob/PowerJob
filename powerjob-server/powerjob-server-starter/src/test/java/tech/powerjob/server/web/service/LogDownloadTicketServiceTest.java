package tech.powerjob.server.web.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.server.auth.jwt.JwtService;
import tech.powerjob.server.auth.jwt.ParseResult;
import tech.powerjob.server.auth.jwt.SecretProvider;
import tech.powerjob.server.auth.jwt.impl.JwtServiceImpl;
import tech.powerjob.server.core.service.CacheService;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LogDownloadTicketServiceTest {
    private LogDownloadTicketService service;
    private JwtService jwt;
    private CacheService cache;

    @BeforeEach
    void setUp() {
        service = new LogDownloadTicketService();
        jwt = mock(JwtService.class);
        cache = mock(CacheService.class);
        ReflectionTestUtils.setField(service, "jwtService", jwt);
        ReflectionTestUtils.setField(service, "cacheService", cache);
        when(cache.getAppIdByInstanceId(42L)).thenReturn(7L);
    }

    private Map<String, Object> claims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("appId", 7L);
        claims.put("instanceId", 42L);
        claims.put("scope", "InstanceLogDownload");
        claims.put("downloadExpiresAt", System.currentTimeMillis() + 60000);
        return claims;
    }

    @Test
    void issueBindsInstanceAndAppWithIndependentFiveMinuteDeadline() {
        when(jwt.build(anyMap(), anyString())).thenReturn("fixture-ticket");
        long before = System.currentTimeMillis();
        assertEquals("fixture-ticket", service.issue(7L, 42L));
        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(jwt).build(captor.capture(), eq("InstanceLogDownload"));
        Map claims = captor.getValue();
        assertEquals(7L, claims.get("appId"));
        assertEquals(42L, claims.get("instanceId"));
        assertTrue((Long) claims.get("downloadExpiresAt") >= before + 300000);
        assertTrue((Long) claims.get("downloadExpiresAt") <= System.currentTimeMillis() + 300000);
    }

    @Test
    void correctTicketWorksAcrossServiceInstancesSharingSigningKey() {
        when(jwt.parse("ticket", "InstanceLogDownload")).thenReturn(new ParseResult().setStatus(ParseResult.Status.SUCCESS).setResult(claims()));
        assertDoesNotThrow(() -> service.validate("ticket", 42L));
    }

    @ParameterizedTest
    @ValueSource(strings = {"expired", "missing-deadline", "other-instance", "other-app", "wrong-scope", "invalid-signature"})
    void rejectsUnboundOrExpiredTicket(String variant) {
        Map<String, Object> claims = claims();
        ParseResult result = new ParseResult().setStatus(ParseResult.Status.SUCCESS).setResult(claims);
        switch (variant) {
            case "expired": claims.put("downloadExpiresAt", System.currentTimeMillis() - 1); break;
            case "missing-deadline": claims.remove("downloadExpiresAt"); break;
            case "other-instance": claims.put("instanceId", 43L); break;
            case "other-app": claims.put("appId", 8L); break;
            case "wrong-scope": claims.put("scope", "web-login"); break;
            default: result.setStatus(ParseResult.Status.FAILED);
        }
        when(jwt.parse("ticket", "InstanceLogDownload")).thenReturn(result);
        assertThrows(PowerJobException.class, () -> service.validate("ticket", 42L));
    }

    @Test
    void noTicketFailsBeforeResourceLookup() {
        assertThrows(PowerJobException.class, () -> service.validate(null, 42L));
        verifyNoInteractions(jwt, cache);
    }

    @Test
    void wrongAppCannotIssueTicket() {
        assertThrows(PowerJobException.class, () -> service.issue(8L, 42L));
        verifyNoInteractions(jwt);
    }

    @Test
    void actualJwtSigningAndParsingSupportTheDownloadDomain() {
        JwtServiceImpl actualJwt = new JwtServiceImpl();
        SecretProvider secretProvider = () -> "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        ReflectionTestUtils.setField(actualJwt, "secretProvider", secretProvider);
        ReflectionTestUtils.setField(actualJwt, "jwtExpireTime", 600);
        ReflectionTestUtils.setField(service, "jwtService", actualJwt);
        String ticket = service.issue(7L, 42L);
        assertDoesNotThrow(() -> service.validate(ticket, 42L));
        assertThrows(PowerJobException.class, () -> service.validate(ticket, 43L));
        assertThrows(PowerJobException.class, () -> service.validate(ticket + "corrupt", 42L));
    }
}
