package tech.powerjob.remote.framework.engine.config;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 代理配置
 *
 * @author tjq
 * @since 2023/11/15
 */
@Data
@Accessors(chain = true)
public class ProxyConfig implements Serializable {
    /**
     * 本机是否初始化代理服务器
     * 当其他服务需要通过代理服务访问本机时，设置为 true，会在本机开启端口提供转发服务
     */
    private boolean enableProxyServer;
    /**
     * 本机启动的代理服务器端口，当 enableProxyServer 为 true 时有效
     */
    private Integer proxyServerPort = 9999;

    /* ******************* 上述配置是本机自身行为，下面的配置是对外的访问行为，请勿混淆 ******************* */
    /**
     * 是否启用代理（去访问其他节点）
     */
    private boolean useProxy;
    /**
     * （访问其他节点的）代理服务器完整的地址
     * 域名 OR IP:port
     */
    private String proxyUrl;
}
