package tech.powerjob.server.auth.login;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * BizUser
 *
 * @author tjq
 * @since 2023/3/21
 */
@Getter
@Setter
@ToString
public class BizUser {

    /**
     * 用户的唯一标识，用于关联到 PowerJob 的 username
     */
    private String username;
}
