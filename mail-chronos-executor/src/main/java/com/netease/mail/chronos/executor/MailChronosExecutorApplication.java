package com.netease.mail.chronos.executor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @author Echo009
 * @since 2021/08/25
 */
@SpringBootApplication
@EnableFeignClients(basePackages = {"com.netease.mail.mp.api"})
public class MailChronosExecutorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MailChronosExecutorApplication.class, args);
    }

}
