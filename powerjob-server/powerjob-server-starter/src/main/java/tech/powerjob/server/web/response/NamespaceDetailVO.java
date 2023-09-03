package tech.powerjob.server.web.response;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

/**
 * 详细命名空间信息，需要权限访问
 *
 * @author tjq
 * @since 2023/9/3
 */
@Getter
@Setter
@ToString(callSuper = true)
public class NamespaceDetailVO extends NamespaceBaseVO {

    /**
     * 访问 token
     */
    private String token;
    /**
     * 有权限的用户
     */
    private Map<String, List<UserBaseVO>> privilegedUsers;
}
