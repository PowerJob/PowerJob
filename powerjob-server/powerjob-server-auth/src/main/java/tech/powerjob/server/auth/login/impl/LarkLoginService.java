package tech.powerjob.server.auth.login.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import tech.powerjob.server.auth.login.LoginTypeInfo;
import tech.powerjob.server.auth.login.ThirdPartyLoginRequest;
import tech.powerjob.server.auth.login.ThirdPartyLoginService;
import tech.powerjob.server.auth.login.ThirdPartyUser;
import tech.powerjob.server.common.Loggers;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 飞书登录服务
 * 参考文档: https://open.feishu.cn/document/common-capabilities/sso/web-application-sso/web-app-overview
 *
 * @author zaki.chen
 * @since 2026/04/02
 */
@ConditionalOnProperty(name = "oms.auth.lark.enable", havingValue = "true")
@Service
public class LarkLoginService implements ThirdPartyLoginService {

    @Value("${oms.auth.lark.app-id:#{null}}")
    private String larkAppId;

    @Value("${oms.auth.lark.app-secret:#{null}}")
    private String larkAppSecret;

    @Value("${oms.auth.lark.callback-url:#{null}}")
    private String larkCallbackUrl;

    @Override
    public LoginTypeInfo loginType() {
        return new LoginTypeInfo()
                .setType("LARK")
                .setName("LarkLogin");
    }

    @Override
    @SneakyThrows
    public String generateLoginUrl(HttpServletRequest httpServletRequest) {
        if (StringUtils.isAnyEmpty(larkAppId, larkAppSecret, larkCallbackUrl)) {
            throw new IllegalArgumentException("please config 'oms.auth.lark.app-id', 'oms.auth.lark.app-secret' and 'oms.auth.lark.callback-url' in properties!");
        }

        String urlString = URLEncoder.encode(larkCallbackUrl, StandardCharsets.UTF_8.name());
        String url = "https://open.feishu.cn/open-apis/authen/v1/authorize?" +
                "app_id=" + larkAppId +
                "&redirect_uri=" + urlString +
                "&state=LARK";
        Loggers.WEB.info("[LarkLoginService] login url: {}", url);
        return url;
    }

    @Override
    @SneakyThrows
    public ThirdPartyUser login(ThirdPartyLoginRequest loginRequest) {
        try {
            String code = loginRequest.getHttpServletRequest().getParameter("code");
            String accessToken = getAccessToken(code);
            JsonObject userInfo = getUserInfo(accessToken);

            ThirdPartyUser bizUser = new ThirdPartyUser();
            bizUser.setUsername(userInfo.get("union_id").getAsString());
            bizUser.setNick(userInfo.get("name").getAsString());

            if (userInfo.has("mobile")) {
                bizUser.setPhone(userInfo.get("mobile").getAsString());
            }
            if (userInfo.has("email")) {
                bizUser.setEmail(userInfo.get("email").getAsString());
            }

            return bizUser;
        } catch (Exception e) {
            Loggers.WEB.error("[LarkLoginService] login failed!", e);
            throw e;
        }
    }

    private String getAppAccessToken() throws Exception {
        URL url = new URL("https://open.feishu.cn/open-apis/auth/v3/app_access_token/internal");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        JsonObject body = new JsonObject();
        body.addProperty("app_id", larkAppId);
        body.addProperty("app_secret", larkAppSecret);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        String response = readResponse(conn);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        return json.get("app_access_token").getAsString();
    }

    private String getAccessToken(String code) throws Exception {
        String appAccessToken = getAppAccessToken();

        URL url = new URL("https://open.feishu.cn/open-apis/authen/v1/access_token");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + appAccessToken);
        conn.setDoOutput(true);

        JsonObject body = new JsonObject();
        body.addProperty("grant_type", "authorization_code");
        body.addProperty("code", code);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        String response = readResponse(conn);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        return json.getAsJsonObject("data").get("access_token").getAsString();
    }

    private JsonObject getUserInfo(String accessToken) throws Exception {
        URL url = new URL("https://open.feishu.cn/open-apis/authen/v1/user_info");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);

        String response = readResponse(conn);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        return json.getAsJsonObject("data");
    }

    private String readResponse(HttpURLConnection conn) throws Exception {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
}
