package com.netease.mail.chronos.base.context;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author Echo009
 * @since 2021/9/30
 */
@EnableFeignClients(basePackages = {"com.netease.mail.mp.api.*"})
@EnableAutoConfiguration
@ComponentScan("com.netease.mail.mp.api.notify")
public class FeignClientBaseContext {




}
