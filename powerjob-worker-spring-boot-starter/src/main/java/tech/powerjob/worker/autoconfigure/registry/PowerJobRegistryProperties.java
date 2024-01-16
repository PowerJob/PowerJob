package tech.powerjob.worker.autoconfigure.registry;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author minsin/mintonzhang@163.com
 * @since 2024/1/16
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "powerjob.registry")
public class PowerJobRegistryProperties {

    /**
     * 自动注册
     * app密码 默认1No2Bug3Thanks
     */
    private String appPassword = "1No2Bug3Thanks";
}
