package tech.powerjob.server.config;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import tech.powerjob.server.netease.config.AuthConfig;
import tech.powerjob.server.netease.interceptor.AuthInterceptor;
import tech.powerjob.server.netease.service.AuthService;

/**
 * CORS
 *
 * @author tjq
 * @since 2020/4/13
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthConfig authConfig;

    private final AuthService authService;

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> configurer(
            @Value("${spring.application.name}") String applicationName) {
        return registry -> registry.config().commonTags("application", applicationName);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowCredentials(true)
                .allowedOrigins("*")
                .allowedOrigins("*")
                .allowedMethods("*");
    }

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor(authService, authConfig))
                .excludePathPatterns("/auth/**","/openApi/**","/health/**","/server/**")
                .addPathPatterns("/**");
    }
}
