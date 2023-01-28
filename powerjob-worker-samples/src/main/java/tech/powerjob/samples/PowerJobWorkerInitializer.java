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
public class PowerJobWorkerInitializer {

    /*
    手动配置版代码
    常规 SpringBoot 用户直接使用 starter 配置即可，具体配置见 application.properties

    @Bean
    public PowerJobSpringWorker initPowerJobSpringWorkerByCode() {

        // 初始化 PowerJob 配置文件
        PowerJobWorkerConfig config = new PowerJobWorkerConfig();
        // 传输协议，新用户建议直接上 HTTP
        config.setProtocol(Protocol.HTTP);
        // 传输层端口号
        config.setPort(28888);
        // worker 的归组，建议使用项目名称
        config.setAppName("powerjob-multi-worker-2");
        // server 的服务发现地址，支持多IP 或 HTTP 域名
        config.setServerAddress(Lists.newArrayList("127.0.0.1:7700", "127.0.0.1:7701"));
        // 如果没有大型 Map/MapReduce 的需求，建议使用内存来加速计算
        config.setStoreStrategy(StoreStrategy.DISK);
        // 执行器的自定义标签，可用于指定部分执行器运行。举例：多单元机房将 TAG 设置为单元名称，即可在控制台指定单元运行
        config.setTag("CENTER");

        // 以上为核心配置，其他配置可直接参考注释 or 官方文档

        // 注意 Spring 用户请使用 PowerJobSpringWorker 而不是 PowerJobWorker，后者无法使用 Spring 管理的 Bean 作为执行器
        return new PowerJobSpringWorker(config);
    }

     */
}
