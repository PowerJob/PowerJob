package tech.powerjob.remote.benchmark;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 测试工程
 * 用于 remote 协议压测
 *
 * @author tjq
 * @since 2023/1/7
 */
@SpringBootApplication
public class BenchmarkApplication {

    public static void main(String[] args) {
        SpringApplication.run(BenchmarkApplication.class, args);
    }
}
