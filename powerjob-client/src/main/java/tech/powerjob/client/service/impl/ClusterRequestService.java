package tech.powerjob.client.service.impl;

import lombok.extern.slf4j.Slf4j;
import tech.powerjob.client.ClientConfig;
import tech.powerjob.client.extension.ClientExtension;
import tech.powerjob.client.extension.ExtensionContext;
import tech.powerjob.client.service.HttpResponse;
import tech.powerjob.client.service.PowerRequestBody;
import tech.powerjob.client.service.RequestService;
import tech.powerjob.common.OpenAPIConstant;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.utils.CollectionUtils;

import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;

/**
 * 集群请求服务
 * 封装网络相关通用逻辑
 *
 * @author tjq
 * @since 2024/2/21
 */
@Slf4j
abstract class ClusterRequestService implements RequestService {

    protected final ClientConfig config;

    /**
     * 当前地址（上次请求成功的地址）
     */
    protected String currentAddress;

    /**
     * 地址格式
     * 协议://域名/OpenAPI/子路径
     */
    protected static final String URL_PATTERN = "%s://%s%s%s";

    /**
     * 默认超时时间
     */
    protected static final Integer DEFAULT_TIMEOUT_SECONDS = 2;

    protected static final int HTTP_SUCCESS_CODE = 200;

    public ClusterRequestService(ClientConfig config) {
        this.config = config;
        this.currentAddress = config.getAddressList().get(0);
    }

    /**
     * 具体某一次 HTTP 请求的实现
     * @param url 完整请求地址
     * @param body 请求体
     * @return 响应
     * @throws IOException 异常
     */
    protected abstract HttpResponse sendHttpRequest(String url, PowerRequestBody body) throws IOException;

    /**
     * 封装集群请求能力
     * @param path 请求 PATH
     * @param powerRequestBody 请求体
     * @return 响应
     */
    protected HttpResponse clusterHaRequest(String path, PowerRequestBody powerRequestBody) {

        // 先尝试默认地址
        String url = getUrl(path, currentAddress);
        try {
            return sendHttpRequest(url, powerRequestBody);
        } catch (IOException e) {
            log.warn("[ClusterRequestService] request url:{} failed, reason is {}.", url, e.toString());
        }

        List<String> addressList = fetchAddressList();

        // 失败，开始重试
        for (String addr : addressList) {
            if (Objects.equals(addr, currentAddress)) {
                continue;
            }
            url = getUrl(path, addr);
            try {
                HttpResponse res = sendHttpRequest(url, powerRequestBody);
                log.warn("[ClusterRequestService] server change: from({}) -> to({}).", currentAddress, addr);
                currentAddress = addr;
                return res;
            } catch (IOException e) {
                log.warn("[ClusterRequestService] request url:{} failed, reason is {}.", url, e.toString());
            }
        }

        log.error("[ClusterRequestService] do post for path: {} failed because of no server available in {}.", path, addressList);
        throw new PowerJobException("no server available when send post request");
    }

    private List<String> fetchAddressList() {

        ClientExtension clientExtension = config.getClientExtension();
        if (clientExtension != null) {
            List<String> addressList = clientExtension.addressProvider(new ExtensionContext());
            if (!CollectionUtils.isEmpty(addressList)) {
                return addressList;
            }
        }

        return config.getAddressList();
    }

    /**
     * 不验证证书
     * X.509 是一个国际标准，定义了公钥证书的格式。这个标准是由国际电信联盟（ITU-T）制定的，用于公钥基础设施（PKI）中数字证书的创建和分发。X.509证书主要用于在公开网络上验证实体的身份，如服务器或客户端的身份验证过程中，确保通信双方是可信的。X.509证书广泛应用于多种安全协议中，包括SSL/TLS，它是实现HTTPS的基础。
     */
    protected static class NoVerifyX509TrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] arg0, String arg1) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] arg0, String arg1) {
            // 不验证
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }


    private String getUrl(String path, String address) {
        String protocol = config.getProtocol().getProtocol();
        return String.format(URL_PATTERN, protocol, address, OpenAPIConstant.WEB_PATH, path);
    }
}
