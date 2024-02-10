package tech.powerjob.server.auth.login;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 登录类型描述
 *
 * @author tjq
 * @since 2024/2/10
 */
@Data
@Accessors(chain = true)
public class LoginTypeInfo implements Serializable {

    /**
     * 登录类型，唯一标识
     */
    private String type;
    /**
     * 描述名称，前端展示用
     */
    private String name;
    /**
     * 展示用的 ICON
     */
    private String iconUrl;
}
