package com.netease.mail.chronos.base.context;

import com.netease.mail.chronos.executor.config.datasource.DataSourceConfig;
import com.netease.mail.mp.api.notify.NotifyApiByDomain;
import com.netease.mail.mp.api.notify.client.NotifyClient;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Echo009
 * @since 2021/10/28
 */
@ComponentScan({"com.netease.mail.chronos.executor.support.service", "com.netease.mail.chronos.executor.support.mapper"})
@Import(DataSourceConfig.class)
@Configuration
public class ServiceBaseContext {


    @Bean
    public NotifyClient notifyClient() {
        return Mockito.mock(NotifyClient.class);
    }

    @Bean
    public NotifyApiByDomain notifyApiByDomain(){
        return Mockito.mock(NotifyApiByDomain.class);
    }

}
