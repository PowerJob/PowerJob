package tech.powerjob.server.web.service;

import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;
import tech.powerjob.common.enums.ErrorCodes;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.server.auth.jwt.JwtService;
import tech.powerjob.server.auth.jwt.ParseResult;
import tech.powerjob.server.core.service.CacheService;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Short-lived, instance-scoped authorization for cross-server log downloads. */
@Service
public class LogDownloadTicketService {

    private static final String SIGNING_SCOPE = "InstanceLogDownload";
    private static final long VALIDITY_MILLIS = 5 * 60 * 1000L;

    @Resource
    private JwtService jwtService;
    @Resource
    private CacheService cacheService;

    public void checkOwner(Long appId, Long instanceId) {
        if (appId == null || instanceId == null || !Objects.equals(appId, cacheService.getAppIdByInstanceId(instanceId))) {
            throw invalidTicket();
        }
    }

    public String issue(Long appId, Long instanceId) {
        checkOwner(appId, instanceId);
        Map<String, Object> claims = new HashMap<>();
        claims.put("appId", appId);
        claims.put("instanceId", instanceId);
        claims.put("scope", SIGNING_SCOPE);
        claims.put("downloadExpiresAt", System.currentTimeMillis() + VALIDITY_MILLIS);
        return jwtService.build(claims, SIGNING_SCOPE);
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
