package com.netease.mail.chronos.base.po;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author Echo009
 * @since 2021/9/26
 */
@Data
@Accessors(chain = true)
public class AuthInfo {

    private String appName;

    private String appSecrets;

}
