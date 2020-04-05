package com.github.kfcfans.oms.server.common.config;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ZooKeeper 连接配置
 *
 * @author tjq
 * @since 2020/4/4
 */
@Configuration
public class CuratorConfig {

    @Value("${zookeeper.address}")
    private String zkAddress;

    @Bean("omsCurator")
    public CuratorFramework initCurator() {
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .namespace("oms")
                // zookeeper 地址，多值用 , 分割即可
                .connectString(zkAddress)
                .sessionTimeoutMs(1000)
                .connectionTimeoutMs(1000)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        client.start();
        return client;
    }

}
