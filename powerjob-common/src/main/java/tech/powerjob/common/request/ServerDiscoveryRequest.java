package tech.powerjob.common.request;

import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import tech.powerjob.common.enums.Protocol;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 服务发现请求
 *
 * @author tjq
 * @since 2023/1/21
 */
@Setter
@Accessors(chain = true)
public class ServerDiscoveryRequest implements Serializable {

    private Long appId;

    private String protocol;

    private String currentServer;

    private String clientVersion;

    public Map<String, Object> toMap() {
        Map<String, Object> ret = new HashMap<>();
        // testMode 下 appId 可能为空，此处不判断会导致 testMode 无法启动 #580
        if (appId != null) {
            ret.put("appId", appId);
        }
        ret.put("protocol", protocol);
        if (StringUtils.isNotEmpty(currentServer)) {
            ret.put("currentServer", currentServer);
        }
        if (StringUtils.isNotEmpty(clientVersion)) {
            ret.put("clientVersion", clientVersion);
        }
        return ret;
    }

    public Long getAppId() {
        return appId;
    }

    public String getProtocol() {
        return Optional.ofNullable(protocol).orElse(Protocol.AKKA.name());
    }

    public String getCurrentServer() {
        return currentServer;
    }

    public String getClientVersion() {
        return clientVersion;
    }
}
