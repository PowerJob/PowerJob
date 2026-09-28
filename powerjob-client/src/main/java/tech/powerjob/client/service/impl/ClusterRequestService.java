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

import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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

    private static final Set<String> READ_ONLY_PATHS = new HashSet<>(Arrays.asList(
            OpenAPIConstant.AUTH_APP, OpenAPIConstant.ASSERT, OpenAPIConstant.EXPORT_JOB,
            OpenAPIConstant.FETCH_JOB, OpenAPIConstant.FETCH_ALL_JOB, OpenAPIConstant.QUERY_JOB,
            OpenAPIConstant.FETCH_INSTANCE_STATUS, OpenAPIConstant.FETCH_INSTANCE_INFO,
            OpenAPIConstant.QUERY_INSTANCE, OpenAPIConstant.FETCH_WORKFLOW,
            OpenAPIConstant.FETCH_WORKFLOW_INSTANCE_INFO));

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
            requireSafeRetry(path, e);
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
                requireSafeRetry(path, e);
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

    protected static boolean isReadOnly(String path) {
        return READ_ONLY_PATHS.contains(path);
    }

    private static void requireSafeRetry(String path, IOException cause) {
        // These failures occur before a connection is established. Other I/O failures may follow a commit.
        if (!isReadOnly(path) && !(cause instanceof ConnectException)
                && !(cause instanceof UnknownHostException) && !(cause instanceof NoRouteToHostException)) {
            throw new PowerJobException("request outcome is unknown; " + path + " was not retried", cause);
        }
    }

    private String getUrl(String path, String address) {
        String protocol = config.getProtocol().getProtocol();
        return String.format(URL_PATTERN, protocol, address, OpenAPIConstant.WEB_PATH, path);
    }
}
