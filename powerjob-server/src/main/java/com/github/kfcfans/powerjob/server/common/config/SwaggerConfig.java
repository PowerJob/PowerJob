package com.github.kfcfans.powerjob.server.common.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import static springfox.documentation.builders.PathSelectors.any;

/**
 * Configuration class for Swagger UI.
 *
 * @author tjq
 * @author Jiang Jining
 * @since 2020/3/29
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {
    
    private final BuildProperties buildProperties;
    
    public SwaggerConfig(@Autowired(required = false) final BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }
    
    @Bean
    public Docket createRestApi() {
        String version = "unknown";
        if (buildProperties != null) {
            String pomVersion = buildProperties.getVersion();
            if (StringUtils.isNotBlank(pomVersion)) {
                version = pomVersion;
            }
        }
        // apiInfo()用来创建该Api的基本信息（这些基本信息会展现在文档页面中
        ApiInfo apiInfo = new ApiInfoBuilder()
                .title("PowerJob")
                .description("Distributed scheduling and computing framework.")
                .license("Apache Licence 2")
                .termsOfServiceUrl("https://github.com/PowerJob/PowerJob")
                .version(version)
                .build();
        
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo)
                // select()函数返回一个ApiSelectorBuilder实例
                .select()
                // 决定了暴露哪些接口给 Swagger
                .paths(any())
                .build();
    }
    
}
