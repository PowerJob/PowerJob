package tech.powerjob.client.service.impl;

import com.google.common.collect.Maps;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import tech.powerjob.client.ClientConfig;
import tech.powerjob.client.common.Protocol;
import tech.powerjob.common.OmsConstant;
import tech.powerjob.common.exception.PowerJobException;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * desc
 *
 * @author tjq
 * @since 2024/2/20
 */
@Slf4j
public class ClusterRequestServiceOkHttp3Impl extends ClusterRequestService {

    private final OkHttpClient okHttpClient;


    public ClusterRequestServiceOkHttp3Impl(ClientConfig config) {
        super(config);

        // 初始化 HTTP 客户端
        if (Protocol.HTTPS.equals(config.getProtocol())) {
            okHttpClient = initHttpsNoVerifyClient();
        } else {
            okHttpClient = initHttpClient();
        }
    }

    @Override
    public String request(String path, Object body) {
        // TODO
        return null;
    }

    @Override
    protected String sendHttpRequest(String url, String payload, Map<String, String> h) throws IOException {

        // 公共 header
        Map<String, String> headers = Maps.newHashMap();
        if (config.getDefaultHeaders() != null) {
            headers.putAll(config.getDefaultHeaders());
        }
        if (h != null) {
            headers.putAll(h);
        }

        MediaType jsonType = MediaType.parse(OmsConstant.JSON_MEDIA_TYPE);
        RequestBody requestBody = RequestBody.create(jsonType, payload);
        Request request = new Request.Builder()
                .post(requestBody)
                .url(url)
                .headers(Headers.of(headers))
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            int responseCode = response.code();
            if (responseCode == HTTP_SUCCESS_CODE) {
                ResponseBody body = response.body();
                if (body == null) {
                    return null;
                }else {
                    return body.string();
                }
            }
            throw new PowerJobException(String.format("http request failed,code=%d", responseCode));
        }
    }

    @SneakyThrows
    private OkHttpClient initHttpClient() {
        OkHttpClient.Builder okHttpBuilder = commonOkHttpBuilder();
        return okHttpBuilder.build();
    }

    @SneakyThrows
    private OkHttpClient initHttpsNoVerifyClient() {

        X509TrustManager trustManager = new NoVerifyX509TrustManager();

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        OkHttpClient.Builder okHttpBuilder = commonOkHttpBuilder();

        // 不需要校验证书
        okHttpBuilder.sslSocketFactory(sslSocketFactory, trustManager);
        // 不校验 url中的 hostname
        okHttpBuilder.hostnameVerifier((String hostname, SSLSession session) -> true);


        return okHttpBuilder.build();
    }

    private OkHttpClient.Builder commonOkHttpBuilder() {
        return new OkHttpClient.Builder()
                // 设置读取超时时间
                .readTimeout(Optional.ofNullable(config.getReadTimeout()).orElse(DEFAULT_TIMEOUT_SECONDS), TimeUnit.SECONDS)
                // 设置写的超时时间
                .writeTimeout(Optional.ofNullable(config.getReadTimeout()).orElse(DEFAULT_TIMEOUT_SECONDS), TimeUnit.SECONDS)
                // 设置连接超时时间
                .connectTimeout(Optional.ofNullable(config.getReadTimeout()).orElse(DEFAULT_TIMEOUT_SECONDS), TimeUnit.SECONDS);
    }

}
