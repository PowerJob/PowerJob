package tech.powerjob.remote.framework.proxy;

import tech.powerjob.remote.framework.base.ServerType;
import tech.powerjob.remote.framework.engine.config.ProxyConfig;
import tech.powerjob.remote.framework.transporter.Transporter;

/**
 * 代理服务
 *
 * @author tjq
 * @since 2023/11/15
 */
public interface ProxyService {

    /**
     * 初始化
     * @param proxyConfig 代理服务
     */
    void initializeProxyServer(ProxyConfig proxyConfig);

    Transporter warpProxyTransporter(ServerType currentServerType);
}
