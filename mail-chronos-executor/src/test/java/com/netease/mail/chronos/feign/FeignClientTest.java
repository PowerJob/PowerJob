package com.netease.mail.chronos.feign;

import com.google.common.collect.Maps;
import com.netease.mail.chronos.base.context.FeignClientBaseContext;
import com.netease.mail.mp.api.notify.client.NotifyClient;
import com.netease.mail.uaInfo.UaInfoContext;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Echo009
 * @since 2021/9/30
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {FeignClientBaseContext.class})
@ActiveProfiles(value = "local")
public class FeignClientTest {

    @Autowired
    private NotifyClient notifyClient;

    /**
     * 这个只用来 debug 验证 ua
     */
    @Test
    @Ignore
    public void load(){

        HashMap<String, Object> fakeUa = Maps.newHashMap();
        fakeUa.put("fakeUa","ignore");
        UaInfoContext.setUaInfo(fakeUa);

        notifyClient.notifyByDomain(0,"0",new ArrayList<>(),"example@163.com");

    }



}
