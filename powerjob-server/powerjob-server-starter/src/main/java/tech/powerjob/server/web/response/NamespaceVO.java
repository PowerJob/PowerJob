package tech.powerjob.server.web.response;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import tech.powerjob.server.web.request.ComponentUserRoleInfo;

/**
 * 基础版本的命名空间
 *
 * @author tjq
 * @since 2023/9/3
 */
@Getter
@Setter
@ToString
public class NamespaceVO extends NamespaceBaseVO {

    private String dept;
    private String tags;

    /**
     * 扩展字段
     */
    private String extra;

    /**
     * 访问 token
     * 仅拥有当前 namespace 权限的访问者可见
     */
    private String token;

    private ComponentUserRoleInfo componentUserRoleInfo;

}
