package tech.powerjob.worker.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tech.powerjob.remote.http.HttpSpringCSInitializer;
import tech.powerjob.remote.http.spring.PowerjobCSMethodProcessor;
import tech.powerjob.remote.http.spring.SpringUtils;

import java.util.List;

/**
 * @author songyinyin
 * @since 2023/2/12 22:23
 */
@Configuration
@AutoConfigureBefore(PowerJobAutoConfiguration.class)
@ConditionalOnClass(HttpSpringCSInitializer.class)
public class PowerJobRemoteAutoConfiguration implements WebMvcConfigurer {

  @Bean
  public SpringUtils powerJobSpringUtils() {
    return new SpringUtils();
  }

  @Bean
  @ConditionalOnMissingBean
  public PowerjobCSMethodProcessor powerjobCSMethodProcessor() {
    return new PowerjobCSMethodProcessor();
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(powerjobCSMethodProcessor());
  }
}
