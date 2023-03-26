package tech.powerjob.server.auth.login.impl;

import com.aliyun.dingtalkcontact_1_0.models.GetUserHeaders;
import com.aliyun.dingtalkcontact_1_0.models.GetUserResponseBody;
import com.aliyun.dingtalkoauth2_1_0.models.GetUserTokenRequest;
import com.aliyun.dingtalkoauth2_1_0.models.GetUserTokenResponse;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.powerjob.common.Loggers;
import tech.powerjob.server.auth.LoginContext;
import tech.powerjob.server.auth.login.BizLoginService;
import tech.powerjob.server.auth.login.BizUser;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * <a href="https://open.dingtalk.com/document/orgapp/tutorial-obtaining-user-personal-information">钉钉账号体系登录第三方网站</a>
 * PowerJob 官方支持钉钉账号体系登录原因：
 * 1. 钉钉作为当下用户体量最大的企业级办公软件，覆盖率足够高，提供钉钉支持能让更多开发者开箱即用
 * 2. 钉钉的 API 设计和 PowerJob 设想一致，算是个最佳实践，其他企业内部的账号体系可参考这套流程进行接入
 *  - PowerJob 重定向到第三方账号体系登陆页 -> 第三方完成登陆 -> 跳转回调 PowerJob auth 接口 -> PowerJob 解析回调登陆信息，完整用户关联
 *
 * @author tjq
 * @since 2023/3/26
 */
@Service
public class DingTalkBizLoginService implements BizLoginService {

    /**
     * 钉钉应用 AppKey
     */
    @Value("${oms.auth.dingtalk.appkey}")
    private String dingTalkAppKey;
    /**
     * 钉钉应用 AppSecret
     */
    @Value("${oms.auth.dingtalk.appSecret}")
    private String dingTalkAppSecret;
    /**
     * 回调地址，powerjob-server 地址 + /user/auth
     * 比如本地调试时为 <a href="http://localhost:7700/user/loginCallback">LocalDemoCallbackUrl</a>
     * 部署后则为 <a href="http://try.powerjob.tech/user/loginCallback">demoCallBackUrl</a>
     */
    @Value("${oms.auth.dingtalk.callbackUrl}")
    private String dingTalkCallbackUrl;

    private static final String DEFAULT_LOGIN_SERVICE = "DingTalk";

    @Override
    public String type() {
        return DEFAULT_LOGIN_SERVICE;
    }

    @Override
    @SneakyThrows
    public String loginUrl() {

        if (StringUtils.isAnyEmpty(dingTalkAppKey, dingTalkAppSecret, dingTalkCallbackUrl)) {
            throw new IllegalArgumentException("please config 'oms.auth.dingtalk.appkey', 'oms.auth.dingtalk.appSecret' and 'oms.auth.dingtalk.callbackUrl' in properties!");
        }

        String urlString = URLEncoder.encode(dingTalkCallbackUrl, StandardCharsets.UTF_8.name());
        String url = "https://login.dingtalk.com/oauth2/auth?" +
                "redirect_uri=" + urlString +
                "&response_type=code" +
                "&client_id=" + dingTalkAppKey +
                "&scope=openid" +
                "&state=DingTalk" +
                "&prompt=consent";
        Loggers.WEB.info("[DingTalkBizLoginService] login url: {}", url);
        return url;
    }

    @Override
    public Optional<BizUser> login(LoginContext loginContext) {
        try {
            com.aliyun.dingtalkoauth2_1_0.Client client = authClient();
            GetUserTokenRequest getUserTokenRequest = new GetUserTokenRequest()
                    //应用基础信息-应用信息的AppKey,请务必替换为开发的应用AppKey
                    .setClientId(dingTalkAppKey)
                    //应用基础信息-应用信息的AppSecret，,请务必替换为开发的应用AppSecret
                    .setClientSecret(dingTalkAppSecret)
                    .setCode(loginContext.getHttpServletRequest().getParameter("authCode"))
                    .setGrantType("authorization_code");
            GetUserTokenResponse getUserTokenResponse = client.getUserToken(getUserTokenRequest);
            //获取用户个人 token
            String accessToken = getUserTokenResponse.getBody().getAccessToken();
            // 查询钉钉用户
            final GetUserResponseBody dingUser = getUserinfo(accessToken);
            // 将钉钉用户的唯一ID 和 PowerJob 账户体系的唯一键 username 关联
            if (dingUser != null) {
                BizUser bizUser = new BizUser();
                bizUser.setUsername(dingUser.getUnionId());
                return Optional.of(bizUser);
            }
        } catch (Exception e) {
            Loggers.WEB.error("[DingTalkBizLoginService] login by dingTalk failed!", e);
        }
        return Optional.empty();
    }

    /* 以下代码均拷自钉钉官网示例 */

    private static com.aliyun.dingtalkoauth2_1_0.Client authClient() throws Exception {
        Config config = new Config();
        config.protocol = "https";
        config.regionId = "central";
        return new com.aliyun.dingtalkoauth2_1_0.Client(config);
    }
    private static com.aliyun.dingtalkcontact_1_0.Client contactClient() throws Exception {
        Config config = new Config();
        config.protocol = "https";
        config.regionId = "central";
        return new com.aliyun.dingtalkcontact_1_0.Client(config);
    }

    private GetUserResponseBody getUserinfo(String accessToken) throws Exception {
        com.aliyun.dingtalkcontact_1_0.Client client = contactClient();
        GetUserHeaders getUserHeaders = new GetUserHeaders();
        getUserHeaders.xAcsDingtalkAccessToken = accessToken;
        //获取用户个人信息，如需获取当前授权人的信息，unionId参数必须传me
        return client.getUserWithOptions("me", getUserHeaders, new RuntimeOptions()).getBody();
    }
}
