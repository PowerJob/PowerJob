package tech.powerjob.server.web.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 修改密码
 *
 * @author tjq
 * @since 2024/2/13
 */
@Data
public class ChangePasswordRequest implements Serializable {

    private String username;

    private String oldPassword;

    private String newPassword;

    private String newPassword2;
}
