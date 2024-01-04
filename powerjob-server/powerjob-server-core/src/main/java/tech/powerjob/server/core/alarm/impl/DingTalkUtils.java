package tech.powerjob.server.core.alarm.impl;

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiGettokenRequest;
import com.dingtalk.api.request.OapiMessageCorpconversationAsyncsendV2Request;
import com.dingtalk.api.request.OapiUserGetByMobileRequest;
import com.dingtalk.api.response.OapiGettokenResponse;
import com.dingtalk.api.response.OapiUserGetByMobileResponse;
import tech.powerjob.common.exception.PowerJobException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 钉钉工具类
 * 工作通知消息：https://ding-doc.dingtalk.com/doc#/serverapi2/pgoxpy
 *
 * @author tjq
 * @since 2020/8/8
 */
@Slf4j
public class DingTalkUtils implements Closeable {

    private String accessToken;

    private final DingTalkClient sendMsgClient;
    private final DingTalkClient accessTokenClient;
    private final DingTalkClient userIdClient;
    private final ScheduledExecutorService scheduledPool;

    private static final long FLUSH_ACCESS_TOKEN_RATE = 6000;
    private static final String GET_TOKEN_URL = "https://oapi.dingtalk.com/gettoken";
    private static final String SEND_URL = "https://oapi.dingtalk.com/topapi/message/corpconversation/asyncsend_v2";
    private static final String GET_USER_ID_URL = "https://oapi.dingtalk.com/user/get_by_mobile";


    public DingTalkUtils(String appKey, String appSecret) {

        this.sendMsgClient = new DefaultDingTalkClient(SEND_URL);
        this.accessTokenClient = new DefaultDingTalkClient(GET_TOKEN_URL);
        this.userIdClient = new DefaultDingTalkClient(GET_USER_ID_URL);

        refreshAccessToken(appKey, appSecret);

        if (StringUtils.isEmpty(accessToken)) {
            throw new PowerJobException("fetch AccessToken failed, please check your appKey & appSecret");
        }

        scheduledPool = Executors.newSingleThreadScheduledExecutor();
        scheduledPool.scheduleAtFixedRate(() -> refreshAccessToken(appKey, appSecret), FLUSH_ACCESS_TOKEN_RATE, FLUSH_ACCESS_TOKEN_RATE, TimeUnit.SECONDS);
    }

    /**
     * 获取 AccessToken，AccessToken 是调用其他接口的基础，有效期 7200 秒，需要不断刷新
     * @param appKey 应用 appKey
     * @param appSecret 应用 appSecret
     */
    private void refreshAccessToken(String appKey, String appSecret) {
        try {
            OapiGettokenRequest req = new OapiGettokenRequest();
            req.setAppkey(appKey);
            req.setAppsecret(appSecret);
            req.setHttpMethod(HttpMethod.GET.name());
            OapiGettokenResponse rsp = accessTokenClient.execute(req);

            if (rsp.isSuccess()) {
                accessToken = rsp.getAccessToken();
            }else {
                log.warn("[DingTalkUtils] flush accessToken failed with req({}),code={},msg={}.", req.getTextParams(), rsp.getErrcode(), rsp.getErrmsg());
            }
        } catch (Exception e) {
            log.warn("[DingTalkUtils] flush accessToken failed.", e);
        }
    }

    public String fetchUserIdByMobile(String mobile) throws Exception {
        OapiUserGetByMobileRequest request = new OapiUserGetByMobileRequest();
        request.setMobile(mobile);

        OapiUserGetByMobileResponse execute = userIdClient.execute(request, accessToken);
        if (execute.isSuccess()) {
            return execute.getUserid();
        }
        log.info("[DingTalkUtils] fetch userId by mobile({}) failed,reason is {}.", mobile, execute.getErrmsg());
        throw new PowerJobException("fetch userId by phone number failed, reason is " + execute.getErrmsg());
    }

    public void sendMarkdownAsync(String title, List<MarkdownEntity> entities, String userList, Long agentId) throws Exception {
        OapiMessageCorpconversationAsyncsendV2Request request = new OapiMessageCorpconversationAsyncsendV2Request();
        request.setUseridList(userList);
        request.setAgentId(agentId);
        request.setToAllUser(false);

        OapiMessageCorpconversationAsyncsendV2Request.Msg msg = new OapiMessageCorpconversationAsyncsendV2Request.Msg();

        StringBuilder mdBuilder=new StringBuilder();
        mdBuilder.append("## ").append(title).append("\n");
        for (MarkdownEntity entity:entities){
            mdBuilder.append("#### ").append(entity.title).append("\n");
            mdBuilder.append("> ").append(entity.detail).append("\n\n");
        }

        msg.setMsgtype("markdown");
        msg.setMarkdown(new OapiMessageCorpconversationAsyncsendV2Request.Markdown());
        msg.getMarkdown().setTitle(title);
        msg.getMarkdown().setText(mdBuilder.toString());
        request.setMsg(msg);

        sendMsgClient.execute(request, accessToken);
    }

    @Override
    public void close() throws IOException {
        scheduledPool.shutdownNow();
    }

    @AllArgsConstructor
    public static final class MarkdownEntity {
        private final String title;
        private final String detail;
    }
}
