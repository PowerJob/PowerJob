package tech.powerjob.client.service.impl;

import com.google.common.collect.Maps;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import tech.powerjob.client.ClientConfig;
import tech.powerjob.client.common.Protocol;
import tech.powerjob.client.service.HttpResponse;
import tech.powerjob.client.service.PowerRequestBody;
import tech.powerjob.common.OmsConstant;
import tech.powerjob.common.serialize.JsonUtils;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * desc
 *
 * @author tjq
 * @since 2024/2/20
 */
@Slf4j
public class ClusterRequestServiceOkHttp3Impl extends AppAuthClusterRequestService {

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
    protected HttpResponse sendHttpRequest(String url, PowerRequestBody powerRequestBody) throws IOException {

        // 添加公共 header
        powerRequestBody.addHeaders(config.getDefaultHeaders());

        Object obj = powerRequestBody.getPayload();

        RequestBody requestBody = null;

        switch (powerRequestBody.getMime()) {
            case APPLICATION_JSON:
                MediaType jsonType = MediaType.parse(OmsConstant.JSON_MEDIA_TYPE);
                String body = obj instanceof String ? (String) obj : JsonUtils.toJSONStringUnsafe(obj);
                requestBody = RequestBody.create(jsonType, body);

                break;
            case APPLICATION_FORM:
                FormBody.Builder formBuilder = new FormBody.Builder();
                Map<String, String> formObj = (Map<String, String>) obj;
                formObj.forEach(formBuilder::add);
                requestBody = formBuilder.build();
        }

        Request request = new Request.Builder()
                .post(requestBody)
                .headers(Headers.of(powerRequestBody.getHeaders()))
                .url(url)
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {

            int code = response.code();
            HttpResponse httpResponse = new HttpResponse()
                    .setCode(code)
                    .setSuccess(code == HTTP_SUCCESS_CODE);

            ResponseBody body = response.body();
            if (body != null) {
                httpResponse.setResponse(body.string());
            }

            Headers respHeaders = response.headers();
            Set<String> headerNames = respHeaders.names();
            Map<String, String> respHeaderMap = Maps.newHashMap();
            headerNames.forEach(hdKey -> respHeaderMap.put(hdKey, respHeaders.get(hdKey)));

            httpResponse.setHeaders(respHeaderMap);

            return httpResponse;
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
                .writeTimeout(Optional.ofNullable(config.getWriteTimeout()).orElse(DEFAULT_TIMEOUT_SECONDS), TimeUnit.SECONDS)
                // 设置连接超时时间
                .connectTimeout(Optional.ofNullable(config.getConnectionTimeout()).orElse(DEFAULT_TIMEOUT_SECONDS), TimeUnit.SECONDS)
                .callTimeout(Optional.ofNullable(config.getConnectionTimeout()).orElse(DEFAULT_TIMEOUT_SECONDS), TimeUnit.SECONDS);
    }

    @Override
    public void close() throws IOException {

        // 关闭 Dispatcher
        okHttpClient.dispatcher().executorService().shutdown();
        // 清理连接池
        okHttpClient.connectionPool().evictAll();
        // 清理缓存（如果有使用）
        Cache cache = okHttpClient.cache();
        if (cache != null) {
            cache.close();
        }
    }
}
