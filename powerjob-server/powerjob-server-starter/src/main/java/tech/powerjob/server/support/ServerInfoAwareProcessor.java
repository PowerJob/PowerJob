package tech.powerjob.server.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.powerjob.server.common.aware.ServerInfoAware;
import tech.powerjob.server.common.module.ServerInfo;
import tech.powerjob.server.remote.server.self.ServerInfoService;

import java.util.List;

/**
 * ServerInfoAwareProcessor
 *
 * @author tjq
 * @since 2022/9/12
 */
@Slf4j
@Component
public class ServerInfoAwareProcessor {

    public ServerInfoAwareProcessor(ServerInfoService serverInfoService, List<ServerInfoAware> awareList) {
        final ServerInfo serverInfo = serverInfoService.fetchServiceInfo();
        log.info("[ServerInfoAwareProcessor] current server info: {}", serverInfo);
        awareList.forEach(aware -> {
            aware.setServerInfo(serverInfo);
            log.info("[ServerInfoAwareProcessor] set ServerInfo for: {} successfully", aware);
        });
    }
}
