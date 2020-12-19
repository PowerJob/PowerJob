package com.github.kfcfans.powerjob.server.service.id;

import com.github.kfcfans.powerjob.server.persistence.core.repository.ServerInfoRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author user
 */
@Configuration
public class ServerIdProviderConfiguration {
  @ConditionalOnProperty(prefix = "oms.server-id", name = "provider", havingValue = "hostname")
  @Bean(name = "serverIdProvider")
  public ServerIdProvider statefulSetServerIdProvider() {
    return new StatefulSetServerIdProvider();
  }

  @ConditionalOnMissingBean(ServerIdProvider.class)
  @Bean(name = "serverIdProvider")
  public ServerIdProvider defaultServerIdProvider(ServerInfoRepository serverInfoRepository) {
    return new DefaultServerIdProvider(serverInfoRepository);
  }
}
