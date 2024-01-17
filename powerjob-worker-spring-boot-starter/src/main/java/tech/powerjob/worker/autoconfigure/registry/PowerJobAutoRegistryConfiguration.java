package tech.powerjob.worker.autoconfigure.registry;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.powerjob.client.IPowerJobClient;
import tech.powerjob.client.PowerJobClient;
import tech.powerjob.worker.autoconfigure.PowerJobProperties;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * 要开启自动注册需要引入client的包,就会自动注册
 *
 * @author minsin/mintonzhang@163.com
 * @since 2024/1/16
 */
@ConditionalOnClass(IPowerJobClient.class)
@ConditionalOnProperty(prefix = "powerjob.registry", name = "enable-auto-registry", havingValue = "true", matchIfMissing = true)
@Configuration
@AutoConfigureBefore(PowerJobAutoRegistryConfiguration.class)
@RequiredArgsConstructor
@Slf4j
public class PowerJobAutoRegistryConfiguration {

    private final PowerJobProperties powerJobProperties;

    @Bean
    @ConditionalOnMissingBean
    public IPowerJobClient powerJobClient() {
        PowerJobProperties.Worker worker = powerJobProperties.getWorker();
        PowerJobProperties.Registry registry = powerJobProperties.getRegistry();
        //尝试注册app
        ArrayList<String> addresses = Lists.newArrayList(StringUtils.split(worker.getServerAddress(), ","));
        RegistryAppUtils.tryRegisterApp(worker.getAppName(), registry.getAppPassword(), addresses);

        return new PowerJobClient(addresses, worker.getAppName(), registry.getAppPassword());
    }

    @Bean
    @ConditionalOnMissingBean
    public AutoRegistryJobBean autoRegistryJob(ObjectProvider<List<BasicProcessor>> processorList, IPowerJobClient powerJobClient) {
        return new AutoRegistryJobBean(processorList, powerJobProperties, powerJobClient);
    }

}
