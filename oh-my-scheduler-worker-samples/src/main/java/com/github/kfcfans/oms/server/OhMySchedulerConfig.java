package com.github.kfcfans.oms.server;

import com.github.kfcfans.oms.worker.OhMyWorker;
import com.github.kfcfans.oms.worker.common.OhMyConfig;
import com.github.kfcfans.oms.worker.common.constants.StoreStrategy;
import com.google.common.collect.Lists;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OMS-Worker 配置
 *
 * @author tjq
 * @since 2020/4/17
 */
@Configuration
public class OhMySchedulerConfig {

    @Bean
    public OhMyWorker initOMS() throws Exception {

        List<String> serverAddress = Lists.newArrayList("192.168.1.6:7700", "127.0.0.1:7700");

        // 1. 创建配置文件
        OhMyConfig config = new OhMyConfig();
        config.setAppName("oms-test");
        config.setServerAddress(serverAddress);
        config.setStoreStrategy(StoreStrategy.DISK);

        // 2. 创建 Worker 对象，设置配置文件
        OhMyWorker ohMyWorker = new OhMyWorker();
        ohMyWorker.setConfig(config);
        return ohMyWorker;
    }

}
