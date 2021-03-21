package tech.powerjob.worker.autoconfigure;

import tech.powerjob.worker.PowerJobWorker;
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
        PowerJobWorker worker = run.getBean(PowerJobWorker.class);
        Assertions.assertNotNull(worker);
    }

}
