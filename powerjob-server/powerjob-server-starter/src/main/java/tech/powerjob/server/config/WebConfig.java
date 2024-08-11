package tech.powerjob.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import tech.powerjob.common.OpenAPIConstant;
import tech.powerjob.server.auth.interceptor.PowerJobAuthInterceptor;
import tech.powerjob.server.openapi.OpenApiInterceptor;

import javax.annotation.Resource;

/**
 * CORS
 *
 * @author tjq
 * @since 2020/4/13
 */
@Configuration
@EnableWebSocket
public class WebConfig implements WebMvcConfigurer {

    @Resource
    private OpenApiInterceptor openApiInterceptor;
    @Resource
    private PowerJobAuthInterceptor powerJobAuthInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedMethods("HEAD", "GET", "PUT", "POST", "DELETE", "PATCH");
    }

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        /*
        可以添加多个拦截器
        addPathPatterns("/**") 表示对所有请求都拦截
        .excludePathPatterns("/base/index") 表示排除对/base/index请求的拦截
        多个拦截器可以设置order顺序，值越小，preHandle越先执行，postHandle和afterCompletion越后执行
        order默认的值是0，如果只添加一个拦截器，可以不显示设置order的值
         */
        registry.addInterceptor(powerJobAuthInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/css/**", "/js/**", "/images/**", "/img/**", "/fonts/**", "/favicon.ico")
                .order(0);

        registry.addInterceptor(openApiInterceptor)
                .addPathPatterns(OpenAPIConstant.WEB_PATH.concat("/**"))
                .order(1);
    }
}
