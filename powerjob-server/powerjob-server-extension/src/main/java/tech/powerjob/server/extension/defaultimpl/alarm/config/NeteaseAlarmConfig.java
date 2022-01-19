package tech.powerjob.server.extension.defaultimpl.alarm.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author Echo009
 * @since 2022/1/19
 */
@Data
@Configuration
@ComponentScan("com.netease.mail.mp.api.notify")
public class NeteaseAlarmConfig {

    @Value("${alarm.env:test}")
    private String env;

    @Value("${alarm.address}")
    private String address;

    @Value("${alarm.popoAccount}")
    private String popoAccount;


}
