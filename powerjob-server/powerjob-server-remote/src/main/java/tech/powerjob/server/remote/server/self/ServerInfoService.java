package tech.powerjob.server.remote.server.self;

import tech.powerjob.server.common.module.ServerInfo;

/**
 * ServerInfoService
 *
 * @author tjq
 * @since 2022/9/12
 */
public interface ServerInfoService {

    /**
     * fetch current server info
     * @return ServerInfo
     */
    ServerInfo fetchServiceInfo();

}
