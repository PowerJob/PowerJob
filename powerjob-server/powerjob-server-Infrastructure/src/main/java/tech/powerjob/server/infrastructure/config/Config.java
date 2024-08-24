package tech.powerjob.server.infrastructure.config;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 配置对象
 *
 * @author tjq
 * @since 2024/8/24
 */
@Data
@Accessors(chain = true)
public class Config implements Serializable {

    private String key;

    private String value;

    private String comment;
}
