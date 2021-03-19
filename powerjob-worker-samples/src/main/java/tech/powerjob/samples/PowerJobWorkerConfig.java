package tech.powerjob.samples;

import org.springframework.context.annotation.Configuration;

/**
 * powerjob-worker 配置
 * 代码配置示例，SpringBoot 项目支持使用 starter，只需要在 application.properties 中完成配置即可
 *
 * @author tjq
 * @since 2020/4/17
 */
@Configuration
public class PowerJobWorkerConfig {

    /*

    @Bean(name = "worker2")
    public OhMyWorker initOMS() throws Exception {

        // 服务器HTTP地址（端口号为 server.port，而不是 ActorSystem port）
        List<String> serverAddress = Lists.newArrayList("127.0.0.1:7700", "127.0.0.1:7701");

        // 1. 创建配置文件
        OhMyConfig config = new OhMyConfig();
        config.setPort(28888);
        config.setAppName("powerjob-multi-worker-2");
        config.setServerAddress(serverAddress);
        // 如果没有大型 Map/MapReduce 的需求，建议使用内存来加速计算
        config.setStoreStrategy(StoreStrategy.DISK);

        // 2. 创建 Worker 对象，设置配置文件
        OhMyWorker ohMyWorker = new OhMyWorker();
        ohMyWorker.setConfig(config);
        return ohMyWorker;
    }


     */


}
