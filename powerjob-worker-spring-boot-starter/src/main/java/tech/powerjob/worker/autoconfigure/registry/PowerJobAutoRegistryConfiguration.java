package tech.powerjob.worker.autoconfigure.registry;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
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
import tech.powerjob.worker.sdk.RegistryAppUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
public class PowerJobAutoRegistryConfiguration implements InitializingBean {

    private final PowerJobProperties powerJobProperties;

    @Bean
    @ConditionalOnMissingBean
    public IPowerJobClient powerJobClient() {
        PowerJobProperties.Worker worker = powerJobProperties.getWorker();
        PowerJobProperties.Registry registry = powerJobProperties.getRegistry();
        ArrayList<String> addresses = Lists.newArrayList(StringUtils.split(worker.getServerAddress(), ","));
        return new PowerJobClient(addresses, worker.getAppName(), registry.getAppPassword());
    }

    @Bean
    @ConditionalOnMissingBean
    public AutoRegistryJobBean autoRegistryJob(ObjectProvider<List<BasicProcessor>> processorList, IPowerJobClient powerJobClient) {
        return new AutoRegistryJobBean(processorList, powerJobProperties, powerJobClient);
    }

    @Override
    public void afterPropertiesSet() {
        PowerJobProperties.Worker worker = powerJobProperties.getWorker();
        PowerJobProperties.Registry registry = powerJobProperties.getRegistry();
        //尝试注册app
        ArrayList<String> addresses = Lists.newArrayList(StringUtils.split(worker.getServerAddress(), ","));
        AtomicBoolean flag = new AtomicBoolean(false);

        RegistryAppUtils.tryRegisterApp(flag, worker.getAppName(), registry.getAppPassword(), addresses);

        if (flag.get()) {
            log.info("[PowerJobRegistry] 注册App成功,appName:({}),password:({}),serverAddress:({})", worker.getAppName(), registry.getAppPassword(), worker.getServerAddress());
        } else {
            log.warn("[PowerJobRegistry] 自动注册失败,请检查服务端是否存在,appName:({}),password:({}),serverAddress:({})", worker.getAppName(), registry.getAppPassword(), worker.getServerAddress());
        }

    }


}
