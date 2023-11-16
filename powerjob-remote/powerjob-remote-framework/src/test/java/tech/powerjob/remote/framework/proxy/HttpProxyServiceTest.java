package tech.powerjob.remote.framework.proxy;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import tech.powerjob.common.response.AskResponse;
import tech.powerjob.remote.framework.base.URL;
import tech.powerjob.remote.framework.engine.config.ProxyConfig;
import tech.powerjob.remote.framework.proxy.module.ProxyMethod;
import tech.powerjob.remote.framework.proxy.module.ProxyRequest;
import tech.powerjob.remote.framework.proxy.module.ProxyResult;
import tech.powerjob.remote.framework.test.TestTransporter;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * HttpProxyServiceTest
 *
 * @author tjq
 * @since 2023/11/16
 */
@Slf4j
class HttpProxyServiceTest {

    @Test
    @SneakyThrows
    void testHttpProxyService() {

        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setEnableProxyServer(true);

        proxyConfig.setProxyUrl("http://127.0.0.1:9999");
        proxyConfig.setUseProxy(true);


        HttpProxyService httpProxyService = new HttpProxyService(new TestTransporter());

        httpProxyService.initializeProxyServer(proxyConfig);

        URL url = new URL();
        AskResponse askResponse = new AskResponse();
        askResponse.setMessage("from test");

        ProxyRequest tellProxyRequest = new ProxyRequest().setUrl(url).setRequest(askResponse).setProxyMethod(ProxyMethod.TELL.getV());
        CompletionStage<ProxyResult> tellProxyResultCompletionStage = httpProxyService.sendProxyRequest(tellProxyRequest);
        ProxyResult tellProxyResult = tellProxyResultCompletionStage.toCompletableFuture().get(1, TimeUnit.SECONDS);
        log.info("[HttpProxyServiceTest] tellProxyResult: {}", tellProxyResult);

        ProxyRequest askProxyRequest = new ProxyRequest().setUrl(url).setRequest(askResponse).setProxyMethod(ProxyMethod.ASK.getV());
        CompletionStage<ProxyResult> askProxyResultCompletionStage = httpProxyService.sendProxyRequest(askProxyRequest);
        ProxyResult askProxyResult = askProxyResultCompletionStage.toCompletableFuture().get(1, TimeUnit.SECONDS);
        log.info("[HttpProxyServiceTest] askProxyResult: {}", askProxyResult);

        Thread.sleep(1000);
    }

}