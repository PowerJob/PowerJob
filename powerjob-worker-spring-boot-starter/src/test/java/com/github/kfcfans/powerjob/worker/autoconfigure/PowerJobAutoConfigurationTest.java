package com.github.kfcfans.powerjob.worker.autoconfigure;

import com.github.kfcfans.powerjob.worker.OhMyWorker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
class PowerJobAutoConfigurationTest {

    @Test
    void testAutoConfiguration() {
        ConfigurableApplicationContext run = SpringApplication.run(PowerJobAutoConfigurationTest.class);
        OhMyWorker worker = run.getBean(OhMyWorker.class);
        Assertions.assertNotNull(worker);
    }

}
