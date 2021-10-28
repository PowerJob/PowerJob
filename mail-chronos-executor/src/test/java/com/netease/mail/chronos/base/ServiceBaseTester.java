package com.netease.mail.chronos.base;

import com.netease.mail.chronos.base.context.DaoBaseContext;
import com.netease.mail.chronos.base.context.ServiceBaseContext;
import com.netease.mail.mp.api.notify.client.NotifyClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Echo009
 * @since 2021/10/28
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ServiceBaseContext.class})
@ActiveProfiles(value = "local")
@Slf4j
public class ServiceBaseTester {




}
