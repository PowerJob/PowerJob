package tech.powerjob.official.processors.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONValidator;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.log.OmsLogger;
import lombok.Data;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import tech.powerjob.official.processors.CommonBasicProcessor;
import tech.powerjob.official.processors.util.CommonUtils;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * common http processor
 *
 * @author tjq
 * @author Jiang Jining
 * @since 2021/1/30
 */
public class HttpProcessor extends CommonBasicProcessor {

    /**
     * Default timeout is 60 seconds.
     */
    private static final int DEFAULT_TIMEOUT = 60;
    private static final int HTTP_SUCCESS_CODE = 200;
    private static final Map<Integer, OkHttpClient> CLIENT_STORE = new ConcurrentHashMap<>();

    @Override
    public ProcessResult process0(TaskContext taskContext) throws Exception {
        OmsLogger omsLogger = taskContext.getOmsLogger();
        HttpParams httpParams = JSON.parseObject(CommonUtils.parseParams(taskContext), HttpParams.class);

        if (httpParams == null) {
            String message = "httpParams is null, please check jobParam configuration.";
            omsLogger.warn(message);
            return new ProcessResult(false, message);
        }

        if (StringUtils.isEmpty(httpParams.url)) {
            return new ProcessResult(false, "url can't be empty!");
        }

        if (!httpParams.url.startsWith("http")) {
            httpParams.url = "http://" + httpParams.url;
        }
        omsLogger.info("request url: {}", httpParams.url);

        // set default method
        if (StringUtils.isEmpty(httpParams.method)) {
            httpParams.method = "GET";
            omsLogger.info("using default request method: GET");
        } else {
            httpParams.method = httpParams.method.toUpperCase();
            omsLogger.info("request method: {}", httpParams.method);
        }

        // set default mediaType
        if (!"GET".equals(httpParams.method)) {
            // set default request body
            if (StringUtils.isEmpty(httpParams.body)) {
                httpParams.body = new JSONObject().toJSONString();
                omsLogger.warn("try to use default request body:{}", httpParams.body);
            }
            if (JSONValidator.from(httpParams.body).validate() && StringUtils.isEmpty(httpParams.mediaType)) {
                httpParams.mediaType = "application/json";
                omsLogger.warn("try to use 'application/json' as media type");
            }
        }

        // set default timeout
        if (httpParams.timeout == null) {
            httpParams.timeout = DEFAULT_TIMEOUT;
        }
        omsLogger.info("request timeout: {} seconds", httpParams.timeout);
        OkHttpClient client = getClient(httpParams.timeout);

        Request.Builder builder = new Request.Builder().url(httpParams.url);
        if (httpParams.headers != null) {
            httpParams.headers.forEach((k, v) -> {
                builder.addHeader(k, v);
                omsLogger.info("add header {}:{}", k, v);
            });
        }

        switch (httpParams.method) {
            case "PUT":
            case "DELETE":
            case "POST":
                MediaType mediaType = MediaType.parse(httpParams.mediaType);
                omsLogger.info("mediaType: {}", mediaType);
                RequestBody requestBody = RequestBody.create(mediaType, httpParams.body);
                builder.method(httpParams.method, requestBody);
                break;
            default:
                builder.get();
        }

        Response response = client.newCall(builder.build()).execute();
        omsLogger.info("response: {}", response);

        String msgBody = "";
        if (response.body() != null) {
            msgBody = response.body().string();
        }

        int responseCode = response.code();
        String res = String.format("code:%d, body:%s", responseCode, msgBody);
        boolean success = true;
        if (responseCode != HTTP_SUCCESS_CODE) {
            success = false;
            omsLogger.warn("{} url: {} failed, response code is {}, response body is {}",
                    httpParams.method, httpParams.url, responseCode, msgBody);
        }
        return new ProcessResult(success, res);
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
                .connectTimeout(Duration.ZERO)
                .readTimeout(Duration.ZERO)
                .writeTimeout(Duration.ZERO)
                .callTimeout(timeout, TimeUnit.SECONDS)
                .build());
    }
}
