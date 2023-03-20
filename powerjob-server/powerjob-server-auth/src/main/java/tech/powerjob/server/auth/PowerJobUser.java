package tech.powerjob.server.auth;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Set;

/**
 * PowerJob 的 登陆用户
 *
 * @author tjq
 * @since 2023/3/20
 */
@Getter
@Setter
@ToString
public class PowerJobUser implements Serializable {

    private Long id;

    private String username;

    private String extra;

    /* ************** 以上为数据库字段 ************** */

    /**
     * 拥有的权限
     */
    private Set<Role> roles;
}
