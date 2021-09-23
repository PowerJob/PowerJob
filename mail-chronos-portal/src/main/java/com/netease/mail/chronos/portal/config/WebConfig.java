package com.netease.mail.chronos.portal.config;

import com.netease.mail.chronos.portal.handler.CustomExceptionHandler;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/**
 * Web服务配置
 *
 * @author RUIZ
 */
@Configuration
@Import(CustomExceptionHandler.class)
public class WebConfig implements WebMvcConfigurer {


    @Bean
    public MeterRegistryCustomizer<MeterRegistry> configurer(
            @Value("${spring.application.name}") String applicationName) {
        return registry -> registry.config().commonTags("application", applicationName);
    }



}
