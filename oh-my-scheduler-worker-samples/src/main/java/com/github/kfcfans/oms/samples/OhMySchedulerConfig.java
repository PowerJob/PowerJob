package com.github.kfcfans.oms.samples;

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

        // 服务器HTTP地址（端口号为 server.port，而不是 ActorSystem port）
        List<String> serverAddress = Lists.newArrayList("127.0.0.1:7700", "127.0.0.1:7701");

        // 1. 创建配置文件
        OhMyConfig config = new OhMyConfig();
        config.setPort(27777);
        config.setAppName("oms-test");
        config.setServerAddress(serverAddress);
        // 如果没有大型 Map/MapReduce 的需求，建议使用内存来加速计算
        // 为了本地模拟多个实例，只能使用 MEMORY 启动（文件只能由一个应用占有）
        config.setStoreStrategy(StoreStrategy.MEMORY);

        // 2. 创建 Worker 对象，设置配置文件
        OhMyWorker ohMyWorker = new OhMyWorker();
        ohMyWorker.setConfig(config);
        return ohMyWorker;
    }
}
