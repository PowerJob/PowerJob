package tech.powerjob.remote.framework.proxy;

import tech.powerjob.remote.framework.cs.ProxyConfig;

import java.util.concurrent.CompletionStage;

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
    void initialize(ProxyConfig proxyConfig);

    /**
     * 代理请求
     * @param proxyRequest 代理请求
     * @return 代理响应
     */
    CompletionStage<ProxyResult> sendProxyRequest(ProxyRequest proxyRequest);
}
