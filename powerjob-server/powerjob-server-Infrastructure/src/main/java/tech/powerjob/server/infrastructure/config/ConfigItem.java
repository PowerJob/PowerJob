package tech.powerjob.server.infrastructure.config;

import lombok.AllArgsConstructor;
import tech.powerjob.server.common.options.WebOptionAbility;

/**
 * 支持的配置项
 *
 * @author tjq
 * @since 2024/8/24
 */
@AllArgsConstructor
public enum ConfigItem implements WebOptionAbility {

    AUTH_LOGIN_TYPE_BLACKLIST("oms.auth.login-type.blacklist", "需要禁用的登录方式，多值逗号分割")
    ;


    private final String code;

    private final String desc;

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getLabel() {
        return String.format("%s（%s）", code, desc);
    }
}
