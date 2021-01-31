package tech.powerjob.offical.processors.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONValidator;
import com.github.kfcfans.powerjob.worker.core.processor.ProcessResult;
import com.github.kfcfans.powerjob.worker.core.processor.TaskContext;
import com.github.kfcfans.powerjob.worker.log.OmsLogger;
import lombok.Data;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import tech.powerjob.offical.processors.CommonBasicProcessor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * common http processor
 *
 * @author tjq
 * @since 2021/1/30
 */
public class HttpProcessor extends CommonBasicProcessor {

    // 60 seconds
    private static final int DEFAULT_TIMEOUT = 60;
    private static final Map<Integer, OkHttpClient> CLIENT_STORE = new ConcurrentHashMap<>();

    @Override
    public ProcessResult process0(TaskContext taskContext) throws Exception {
        OmsLogger omsLogger = taskContext.getOmsLogger();
        HttpParams httpParams = JSONObject.parseObject(taskContext.getJobParams(), HttpParams.class);

        if (StringUtils.isEmpty(httpParams.url)) {
            return new ProcessResult(false, "url can't be empty!");
        }

        if (!httpParams.url.startsWith("http")) {
            httpParams.url = "http://" + httpParams.url;
        }
        omsLogger.info("[HttpProcessor] request url: {}", httpParams.url);

        // set default method
        if (StringUtils.isEmpty(httpParams.method)) {
            httpParams.method = "GET";
            omsLogger.info("[HttpProcessor] using default request method: GET");
        } else {
            httpParams.method = httpParams.method.toUpperCase();
            omsLogger.info("[HttpProcessor] request method: {}", httpParams.method);
        }

        // set default mediaType
        if (!"GET".equals(httpParams.method) && StringUtils.isEmpty(httpParams.mediaType)) {
            if (JSONValidator.from(httpParams.body).validate()) {
                httpParams.mediaType = "application/json";
                omsLogger.warn("[HttpProcessor] try to use 'application/json' as media type");
            }
        }

        // set default timeout
        if (httpParams.timeout == null) {
            httpParams.timeout = DEFAULT_TIMEOUT;
        }
        omsLogger.info("[HttpProcessor] request timeout: {} seconds", httpParams.timeout);
        OkHttpClient client = getClient(httpParams.timeout);

        Request.Builder builder = new Request.Builder().url(httpParams.url);
        if (httpParams.headers != null) {
            httpParams.headers.forEach((k, v) -> {
                builder.addHeader(k, v);
                omsLogger.info("[HttpProcessor] add header {}:{}", k, v);
            });
        }

        switch (httpParams.method) {
            case "PUT":
            case "DELETE":
            case "POST":
                MediaType mediaType = MediaType.parse(httpParams.mediaType);
                omsLogger.info("[HttpProcessor] mediaType: {}", mediaType);
                RequestBody requestBody = RequestBody.create(mediaType, httpParams.body);
                builder.method(httpParams.method, requestBody);
                break;
            default:
                builder.get();
        }

        Response response = client.newCall(builder.build()).execute();
        omsLogger.info("[HttpProcessor] response: {}", response);

        String msgBody = "";
        if (response.body() != null) {
            msgBody = response.body().string();
        }

        String res = String.format("code:%d,body:%s", response.code(), msgBody);
        omsLogger.info("[HttpProcessor] process result: {}", res);

        return new ProcessResult(true, res);
    }

    @Data
    public static class HttpParams {
        /**
         * POST / GET / PUT / DELETE
         */
        private String method;
        /**
         * the request url
         */
        private String url;
        /**
         * application/json
         * application/xml
         * image/png
         * image/jpeg
         * image/gif
         */
        private String mediaType;

        private String body;

        private Map<String, String> headers;

        /**
         * timeout for complete calls
         */
        private Integer timeout;
    }

    private static OkHttpClient getClient(Integer timeout) {
        return CLIENT_STORE.computeIfAbsent(timeout, ignore -> new OkHttpClient.Builder()
                .callTimeout(timeout, TimeUnit.SECONDS)
                .build());
    }
}
