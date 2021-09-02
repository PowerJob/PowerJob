package tech.powerjob.samples;

import com.google.common.collect.Lists;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.powerjob.worker.PowerJobWorker;
import tech.powerjob.worker.common.PowerJobWorkerConfig;
import tech.powerjob.worker.common.constants.StoreStrategy;

import java.util.List;

/**
 * powerjob-worker 配置
 * 代码配置示例，SpringBoot 项目支持使用 starter，只需要在 application.properties 中完成配置即可
 *
 * @author tjq
 * @since 2020/4/17
 */
@Configuration
public class PowerJobWorkerConfiguration {


    @Bean(name = "worker")
    public PowerJobWorker initWorker() {

        // 服务器HTTP地址（端口号为 server.port，而不是 ActorSystem port）
        List<String> serverAddress = Lists.newArrayList("127.0.0.1:7700", "127.0.0.1:7701");
        // 1. 创建配置文件
        PowerJobWorkerConfig config = new PowerJobWorkerConfig();
        config.setPort(28888);
        config.setAppName("powerjob-multi-worker-2");
        config.setServerAddress(serverAddress);
        // 如果没有大型 Map/MapReduce 的需求，建议使用内存来加速计算
        config.setStoreStrategy(StoreStrategy.DISK);
        // 2. 创建 Worker 对象，设置配置文件
        PowerJobWorker powerJobWorker = new PowerJobWorker();
        powerJobWorker.setConfig(config);
        return powerJobWorker;

    }

}
