package com.netease.mail.chronos.executor.config;

import com.netease.mail.chronos.executor.handler.CustomExceptionHandler;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/**
 * Web服务配置
 *
 * @author Echo009
 */
@Configuration
@Import({CustomExceptionHandler.class})
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {


    @Bean
    public MeterRegistryCustomizer<MeterRegistry> configurer(
            @Value("${spring.application.name}") String applicationName) {
        return registry -> registry.config().commonTags("application", applicationName);
    }

}
