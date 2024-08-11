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
     * 密码
     */
    private String password;

    /**
     * webHook
     */
    private String webHook;

    private String originUsername;
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
