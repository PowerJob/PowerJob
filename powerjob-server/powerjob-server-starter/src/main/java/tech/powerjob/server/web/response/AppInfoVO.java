package tech.powerjob.server.web.response;

import lombok.Data;
import tech.powerjob.server.web.request.ComponentUserRoleInfo;

import java.io.Serializable;
import java.util.Date;

/**
 * AppInfoVO
 *
 * @author tjq
 * @since 2024/2/12
 */
@Data
public class AppInfoVO implements Serializable {

    private Long id;

    private String appName;

    /**
     * 描述
     */
    private String title;

    private String password;

    private String tags;

    private String extra;

    private ComponentUserRoleInfo componentUserRoleInfo;

    private Date gmtCreate;

    private String gmtCreateStr;

    private Date gmtModified;

    private String gmtModifiedStr;

    private String creator;

    private String modifier;
}
