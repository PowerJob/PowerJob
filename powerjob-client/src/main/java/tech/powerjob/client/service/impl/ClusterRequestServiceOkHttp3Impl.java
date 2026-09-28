package tech.powerjob.client.service.impl;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.BufferedSink;
import tech.powerjob.client.ClientConfig;
import tech.powerjob.client.service.HttpResponse;
import tech.powerjob.client.service.PowerRequestBody;
import tech.powerjob.common.OmsConstant;
import tech.powerjob.common.OpenAPIConstant;
import tech.powerjob.common.serialize.JsonUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
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

        // Use the platform trust store and OkHttp's default hostname verification for HTTPS.
        okHttpClient = initHttpClient();
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

        String path = HttpUrl.get(url).encodedPath().substring(OpenAPIConstant.WEB_PATH.length());
        if (requestBody != null && !isReadOnly(path)) {
            final RequestBody delegate = requestBody;
            requestBody = new RequestBody() {
                @Override public MediaType contentType() { return delegate.contentType(); }
                @Override public long contentLength() throws IOException { return delegate.contentLength(); }
                @Override public void writeTo(BufferedSink sink) throws IOException { delegate.writeTo(sink); }
                // Also suppress HTTP follow-ups such as 503 + Retry-After: 0, independent of connection retries.
                @Override public boolean isOneShot() { return true; }
            };
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
            Map<String, String> respHeaderMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
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

    private OkHttpClient.Builder commonOkHttpBuilder() {
        return new OkHttpClient.Builder()
                // Retry decisions must account for the OpenAPI operation and whether it may have committed.
                .retryOnConnectionFailure(false)
                // Custom application credentials must not be forwarded to a redirect target.
                .followRedirects(false)
                .followSslRedirects(false)
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
