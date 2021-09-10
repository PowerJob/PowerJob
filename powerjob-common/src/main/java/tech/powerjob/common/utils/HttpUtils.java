package tech.powerjob.common.utils;

import lombok.SneakyThrows;
import tech.powerjob.common.exception.PowerJobException;
import okhttp3.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 封装 OkHttpClient
 *
 * @author tjq
 * @since 2020/4/6
 */
public class HttpUtils {

    private HttpUtils() {

    }

    private static final OkHttpClient CLIENT;

    private static final String FORM_URL_ENCODE = "application/x-www-form-urlencoded";

    private static final int HTTP_SUCCESS_CODE = 200;

    static {
        CLIENT = new OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();
    }

    public static String get(String url) throws IOException {
        Request request = new Request.Builder()
                .get()
                .url(url)
                .build();
        return execute(request);
    }

    @SneakyThrows
    public static String get(String url, Map<String, String> params) {
        url = url + "?" + constructParamString(params);
        Request request = new Request.Builder()
                .get()
                .url(url)
                .build();
        return execute(request);
    }


    public static String post(String url, RequestBody requestBody) throws IOException {
        Request request = new Request.Builder()
                .post(requestBody)
                .url(url)
                .build();
        return execute(request);
    }

    private static String execute(Request request) throws IOException {
        try (Response response = CLIENT.newCall(request).execute()) {
            int responseCode = response.code();
            if (responseCode == HTTP_SUCCESS_CODE) {
                ResponseBody body = response.body();
                if (body == null) {
                    return null;
                } else {
                    return body.string();
                }
            }
            throw new PowerJobException(String.format("http request failed,code=%d", responseCode));
        }
    }


    @SneakyThrows
    public static String post(String url, Map<String, String> param) {

        String params = constructParamString(param);

        RequestBody requestBody = RequestBody.create(MediaType.parse(FORM_URL_ENCODE), params);

        Request request = new Request.Builder()
                .post(requestBody)
                .url(url)
                .build();

        return execute(request);


    }


    public static String constructParamString(Map<String, String> param) {
        return param.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + urlEncode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    @SneakyThrows
    public static String urlEncode(String input) {
        return URLEncoder.encode(input, StandardCharsets.UTF_8.name());
    }


}
