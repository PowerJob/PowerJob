package tech.powerjob.server.auth.login;

import lombok.Data;

import java.io.Serializable;

/**
 * JWT 登录时的校验信息
 *
 * @author tjq
 * @since 2024/2/16
 */
@Data
public class TokenLoginVerifyInfo implements Serializable {

    /**
     * 加密 token 部分，比如密码的 md5，会直接写入 JWT 下发给前端
     * 如果需要使用 JWT 二次校验，则该参数必须存在
     */
    private String encryptedToken;

    /**
     * 补充信息，用于二次校验
     */
    private String additionalInfo;

    /**
     * 依然是预留字段，第三方实现自用即可
     */
    private String extra;
}
