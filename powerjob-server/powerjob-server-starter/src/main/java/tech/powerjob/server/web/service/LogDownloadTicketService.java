package tech.powerjob.server.web.service;

import org.springframework.stereotype.Service;
import tech.powerjob.server.auth.jwt.JwtService;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * Compatibility issuer for log URLs routed to a 5.1.4/5.1.5 Server during a rolling upgrade.
 * New download endpoints do not require this ticket; older receivers still verify its signature
 * using the cluster's existing shared JWT signing key.
 */
@Service
public class LogDownloadTicketService {

    private static final String SIGNING_SCOPE = "InstanceLogDownload";
    private static final long VALIDITY_MILLIS = 5 * 60 * 1000L;

    @Resource
    private JwtService jwtService;

    public String issue(Long appId, Long instanceId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("appId", appId);
        claims.put("instanceId", instanceId);
        claims.put("scope", SIGNING_SCOPE);
        claims.put("downloadExpiresAt", System.currentTimeMillis() + VALIDITY_MILLIS);
        return jwtService.build(claims, SIGNING_SCOPE);
    }
}
