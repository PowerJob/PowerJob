package tech.powerjob.client.module;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Map;

/**
 * App 鉴权响应
 *
 * @author tjq
 * @since 2024/2/21
 */
@Getter
@Setter
@ToString
public class AppAuthResult implements Serializable {

    private Long appId;

    private String token;

    /**
     * 额外参数
     * 有安全需求的开发者可执行扩展
     */
    private Map<String, Object> extra;
}
