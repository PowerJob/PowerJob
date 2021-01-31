package tech.powerjob.offical.processors.impl;

import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import tech.powerjob.offical.processors.TestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HttpProcessorTest
 *
 * @author tjq
 * @since 2021/1/31
 */
class HttpProcessorTest {

    @Test
    void testGet() throws Exception {
        String url = "https://www.baidu.com";
        JSONObject params = new JSONObject();
        params.put("url", url);
        params.put("method", "GET");

        System.out.println(new HttpProcessor().process(TestUtils.genTaskContext(params.toJSONString())));
    }

    @Test
    void testPost() throws Exception {
        String url = "https://mock.uutool.cn/4f5qfgcdahj0?test=true";
        JSONObject params = new JSONObject();
        params.put("url", url);
        params.put("method", "POST");
        params.put("mediaType", "application/json");
        params.put("body", params.toJSONString());

        System.out.println(new HttpProcessor().process(TestUtils.genTaskContext(params.toJSONString())));
    }
}