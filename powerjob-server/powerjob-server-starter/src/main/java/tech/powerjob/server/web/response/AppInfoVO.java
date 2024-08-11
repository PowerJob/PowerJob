package tech.powerjob.server.web.response;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import tech.powerjob.server.web.request.ComponentUserRoleInfo;

import java.util.Date;

/**
 * AppInfoVO
 *
 * @author tjq
 * @since 2024/2/12
 */
@Getter
@Setter
@ToString
public class AppInfoVO extends AppBaseVO {

    private String password;

    private String tags;

    private String extra;

    private ComponentUserRoleInfo componentUserRoleInfo;

    private Date gmtCreate;

    private String gmtCreateStr;

    private Date gmtModified;

    private String gmtModifiedStr;

    private String creatorShowName;

    private String modifierShowName;

    /**
     * Namespace Info
     */
    private NamespaceBaseVO namespace;

    private String namespaceName;
}
