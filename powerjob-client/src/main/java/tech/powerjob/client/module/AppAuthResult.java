package tech.powerjob.client.module;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

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

    private String extra;
}
