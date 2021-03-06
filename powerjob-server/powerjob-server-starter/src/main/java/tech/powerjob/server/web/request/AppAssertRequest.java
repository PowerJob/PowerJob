package tech.powerjob.server.web.request;

import lombok.Data;

/**
 * 验证应用（应用登陆）
 *
 * @author tjq
 * @since 2020/6/20
 */
@Data
public class AppAssertRequest {
    private String appName;
    private String password;
}
