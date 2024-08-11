package tech.powerjob.client.module;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Map;

/**
 * App 鉴权请求
 *
 * @author tjq
 * @since 2024/2/19
 */
@Getter
@Setter
@ToString
public class AppAuthRequest implements Serializable {

    /**
     * 应用名称
     */
    private String appName;
    /**
     * 加密后密码
     */
    private String encryptedPassword;

    /**
     * 加密类型
     */
    private String encryptType;

    /**
     * 额外参数，方便开发者传递其他参数
     */
    private Map<String, Object> extra;
}
