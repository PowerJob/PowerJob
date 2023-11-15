package tech.powerjob.remote.framework.proxy;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import tech.powerjob.common.PowerSerializable;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.remote.framework.base.RemotingException;
import tech.powerjob.remote.framework.base.URL;
import tech.powerjob.remote.framework.cs.CSInitializerConfig;
import tech.powerjob.remote.framework.cs.ProxyConfig;
import tech.powerjob.remote.framework.transporter.Protocol;
import tech.powerjob.remote.framework.transporter.Transporter;

import java.util.Objects;
import java.util.concurrent.CompletionStage;

/**
 * 具有代理功能的通讯器
 *
 * @author tjq
 * @since 2023/11/15
 */
@Slf4j
public class ProxyTransporter implements Transporter {

    private final ProxyService proxyService;
    private final Transporter realTransporter;
    private final CSInitializerConfig csInitializerConfig;

    public ProxyTransporter(ProxyService proxyService, Transporter realTransporter, CSInitializerConfig csInitializerConfig) {
        this.proxyService = proxyService;
        this.realTransporter = realTransporter;
        this.csInitializerConfig = csInitializerConfig;
    }

    @Override
    public Protocol getProtocol() {
        return realTransporter.getProtocol();
    }

    @Override
    public void tell(URL url, PowerSerializable request) {
        if (skipProxy(url)) {
            realTransporter.tell(url, request);
            return;
        }

        ProxyRequest proxyRequest = new ProxyRequest().setUrl(url).setRequest(request).setProxyMethod(ProxyMethod.TELL.getV());
        proxyService.sendProxyRequest(proxyRequest);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> CompletionStage<T> ask(URL url, PowerSerializable request, Class<T> clz) throws RemotingException {
        if (skipProxy(url)) {
            return realTransporter.ask(url, request, clz);
        }
        ProxyRequest proxyRequest = new ProxyRequest().setUrl(url).setRequest(request).setProxyMethod(ProxyMethod.ASK.getV());
        CompletionStage<ProxyResult> proxyRequestCompletionStage = proxyService.sendProxyRequest(proxyRequest);
        return proxyRequestCompletionStage.thenApply(pr -> {
            if (pr.isSuccess()) {
                if (clz == null) {
                    return null;
                }
                if (clz.equals(String.class)) {
                    return (T) pr.getData();
                }
                try {
                    return JsonUtils.parseObject(pr.getData(), clz);
                } catch (Exception e) {
                    ExceptionUtils.rethrow(e);
                }
            }
            throw new RemotingException("proxy failed, msg: " + pr.getMsg());
        });
    }

    private boolean skipProxy(URL url) {

        ProxyConfig proxyConfig = csInitializerConfig.getProxyConfig();
        if (proxyConfig == null) {
            return true;
        }
        if (!proxyConfig.isUseProxy()) {
            return true;
        }
        if (StringUtils.isEmpty(proxyConfig.getProxyUrl())) {
            return true;
        }

        // 仅对向通讯需要使用代理
        return Objects.equals(url.getServerType(), csInitializerConfig.getServerType());
    }

}
