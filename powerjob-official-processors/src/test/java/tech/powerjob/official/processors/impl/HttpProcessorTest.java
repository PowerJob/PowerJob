package tech.powerjob.official.processors.impl;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.Test;
import tech.powerjob.official.processors.TestUtils;

import java.util.HashMap;

/**
 * HttpProcessorTest
 *
 * @author tjq
 * @since 2021/1/31
 */
@SuppressWarnings("squid:S5976")
class HttpProcessorTest {

    private static final HttpProcessor PROCESSOR = new HttpProcessor();

    @Test
    void testResultJudge1() throws Exception {
        HashMap<String, Object> params = Maps.newHashMap();
        params.put("opt","check");
        params.put("uid","example@163.com");
        HttpProcessor.HttpParams httpParams = new HttpProcessor.HttpParams();
        httpParams.setUrl("https://www.baidu.com");
        httpParams.setMediaType("application/json");
        httpParams.setMethod("POST");
        httpParams.setResultJudgeSpEl("#responseCode.equals(200)");
        httpParams.setTimeout(30);
        httpParams.setBody(JSONObject.toJSONString(params));
        String paramStr = JSONObject.toJSONString(httpParams);
        System.out.println(paramStr);
        System.out.println(PROCESSOR.process(TestUtils.genTaskContext(paramStr)));
    }



    @Test
    void testResultJudge2() throws Exception {
        HttpProcessor.HttpParams httpParams = new HttpProcessor.HttpParams();
        httpParams.setUrl("http://mail.163.com/fgw/mailsrv-peacock/account/recommend");
        httpParams.setMethod("GET");
        httpParams.setResultJudgeSpEl("#responseBodyObject.get('code').equals(200)");
        httpParams.setTimeout(30);
        String paramStr = JSONObject.toJSONString(httpParams);
        System.out.println(paramStr);
        System.out.println(PROCESSOR.process(TestUtils.genTaskContext(paramStr)));

    }

    
    @Test
    void testDefaultMethod() throws Exception {
        String url = "https://www.baidu.com";
        JSONObject params = new JSONObject();
        params.put("url", url);
        System.out.println(PROCESSOR.process(TestUtils.genTaskContext(params.toJSONString())));
    }

    @Test
    void testGet() throws Exception {
        String url = "https://www.baidu.com";
        JSONObject params = new JSONObject();
        params.put("url", url);
        params.put("method", "GET");

        System.out.println(PROCESSOR.process(TestUtils.genTaskContext(params.toJSONString())));
    }

    @Test
    void testPost() throws Exception {
        String url = "https://mock.uutool.cn/4f5qfgcdahj0?test=true";
        JSONObject params = new JSONObject();
        params.put("url", url);
        params.put("method", "POST");
        params.put("mediaType", "application/json");
        params.put("body", params.toJSONString());

        System.out.println(PROCESSOR.process(TestUtils.genTaskContext(params.toJSONString())));
    }
    
    @Test
    void testPostDefaultJson() throws Exception {
        String url = "https://mock.uutool.cn/4f5qfgcdahj0?test=true";
        JSONObject params = new JSONObject();
        params.put("url", url);
        params.put("method", "POST");
        System.out.println(PROCESSOR.process(TestUtils.genTaskContext(params.toJSONString())));
    }
    
    @Test
    void testPostDefaultWithMediaType() throws Exception {
        String url = "https://mock.uutool.cn/4f5qfgcdahj0?test=true";
        JSONObject params = new JSONObject();
        params.put("url", url);
        params.put("method", "POST");
        params.put("mediaType", "application/json");
        System.out.println(PROCESSOR.process(TestUtils.genTaskContext(params.toJSONString())));
    }

    @Test
    void testTimeout() throws Exception {
        String url = "http://localhost:7700/tmp/sleep";
        JSONObject params = new JSONObject();
        params.put("url", url);
        params.put("method", "GET");
        params.put("timeout", 20);
        System.out.println(PROCESSOR.process(TestUtils.genTaskContext(params.toJSONString())));
    }
}