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
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.server.auth.common.AuthConstants;
import tech.powerjob.server.auth.login.*;
import tech.powerjob.server.common.Loggers;

import javax.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
public class DingTalkLoginService implements ThirdPartyLoginService {

    /*
    配置示例
    oms.auth.dingtalk.appkey=dinggzqqzqqzqqzqq
    oms.auth.dingtalk.appSecret=iY-FS8mzqqzqq_xEizqqzqqzqqzqqzqqzqqYEbkZOal
    oms.auth.dingtalk.callbackUrl=http://localhost:7700
     */

    /**
     * 钉钉应用 AppKey
     */
    @Value("${oms.auth.dingtalk.appkey:#{null}}")
    private String dingTalkAppKey;
    /**
     * 钉钉应用 AppSecret
     */
    @Value("${oms.auth.dingtalk.appSecret:#{null}}")
    private String dingTalkAppSecret;
    /**
     * 回调地址，powerjob 前端控制台地址，即 powerjob-console 地址
     * 比如本地调试时为 <a href="http://localhost:7700">LocalDemoCallbackUrl</a>
     * 部署后则为 <a href="http://try.powerjob.tech">demoCallBackUrl</a>
     */
    @Value("${oms.auth.dingtalk.callbackUrl:#{null}}")
    private String dingTalkCallbackUrl;

    @Override
    public LoginTypeInfo loginType() {
        return new LoginTypeInfo()
                .setType(AuthConstants.ACCOUNT_TYPE_DING)
                .setName("DingTalk")
                ;
    }

    @Override
    @SneakyThrows
    public String generateLoginUrl(HttpServletRequest httpServletRequest) {
        if (StringUtils.isAnyEmpty(dingTalkAppKey, dingTalkAppSecret, dingTalkCallbackUrl)) {
            throw new IllegalArgumentException("please config 'oms.auth.dingtalk.appkey', 'oms.auth.dingtalk.appSecret' and 'oms.auth.dingtalk.callbackUrl' in properties!");
        }

        String urlString = URLEncoder.encode(dingTalkCallbackUrl, StandardCharsets.UTF_8.name());
        String url = "https://login.dingtalk.com/oauth2/auth?" +
                "redirect_uri=" + urlString +
                "&response_type=code" +
                "&client_id=" + dingTalkAppKey +
                "&scope=openid" +
                "&state=" + AuthConstants.ACCOUNT_TYPE_DING +
                "&prompt=consent";
        Loggers.WEB.info("[DingTalkBizLoginService] login url: {}", url);
        return url;
    }

    @Override
    @SneakyThrows
    public ThirdPartyUser login(ThirdPartyLoginRequest loginRequest) {
        try {
            com.aliyun.dingtalkoauth2_1_0.Client client = authClient();
            GetUserTokenRequest getUserTokenRequest = new GetUserTokenRequest()
                    //应用基础信息-应用信息的AppKey,请务必替换为开发的应用AppKey
                    .setClientId(dingTalkAppKey)
                    //应用基础信息-应用信息的AppSecret，,请务必替换为开发的应用AppSecret
                    .setClientSecret(dingTalkAppSecret)
                    .setCode(loginRequest.getHttpServletRequest().getParameter("authCode"))
                    .setGrantType("authorization_code");
            GetUserTokenResponse getUserTokenResponse = client.getUserToken(getUserTokenRequest);
            //获取用户个人 token
            String accessToken = getUserTokenResponse.getBody().getAccessToken();
            // 查询钉钉用户
            final GetUserResponseBody dingUser = getUserinfo(accessToken);
            // 将钉钉用户的唯一ID 和 PowerJob 账户体系的唯一键 username 关联
            if (dingUser != null) {
                ThirdPartyUser bizUser = new ThirdPartyUser();
                bizUser.setUsername(dingUser.getUnionId());
                bizUser.setNick(dingUser.getNick());
                bizUser.setPhone(dingUser.getMobile());
                bizUser.setEmail(dingUser.getEmail());
                return bizUser;
            }
        } catch (Exception e) {
            Loggers.WEB.error("[DingTalkBizLoginService] login by dingTalk failed!", e);
            throw e;
        }
        throw new PowerJobException("login from dingTalk failed!");
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
