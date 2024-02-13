package tech.powerjob.server.web.response;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

/**
 * 用户详细信息
 *
 * @author tjq
 * @since 2024/2/13
 */
@Getter
@Setter
@ToString
public class UserDetailVO extends UserBaseVO {

    /**
     * 账户类型
     */
    private String accountType;
    /**
     * 密码
     */
    private String password;

    /**
     * 手机号
     */
    private String phone;
    /**
     * 邮箱地址
     */
    private String email;
    /**
     * webHook
     */
    private String webHook;
    /**
     * 扩展字段
     */
    private String extra;

    /**
     * 拥有的全局权限
     */
    private List<String> globalRoles;
    /**
     * 拥有的 namespace 权限
     */
    private Map<String, List<NamespaceBaseVO>> role2NamespaceList;
    /**
     * 拥有的 app 权限
     */
    private Map<String, List<AppBaseVO>> role2AppList;

}
