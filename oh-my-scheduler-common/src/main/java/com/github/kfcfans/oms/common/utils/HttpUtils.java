package com.github.kfcfans.oms.common.utils;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 封装 OkHttpClient
 *
 * @author tjq
 * @since 2020/4/6
 */
public class HttpUtils {

    private static OkHttpClient client;
    private static final int HTTP_SUCCESS_CODE = 200;

    static {
        client = new OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();
    }

    public static String get(String url) throws IOException {
        Request request = new Request.Builder()
                .get()
                .url(url)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.code() == HTTP_SUCCESS_CODE) {
                return Objects.requireNonNull(response.body()).string();
            }
        }
        return null;
    }

    public static String post(String url, RequestBody requestBody) throws IOException {
        Request request = new Request.Builder()
                .post(requestBody)
                .url(url)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.code() == HTTP_SUCCESS_CODE) {
                return Objects.requireNonNull(response.body()).string();
            }
        }
        return null;
    }

}
