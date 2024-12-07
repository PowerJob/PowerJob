package tech.powerjob.server.web.request;

import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.utils.CommonUtils;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * 修改应用信息请求
 *
 * @author tjq
 * @since 2020/4/1
 */
@Data
public class ModifyAppInfoRequest {

    private Long id;
    private String appName;

    /**
     * namespace 唯一标识，任选其一传递即可
     */
    private Long namespaceId;
    private String namespaceCode;

    private String oldPassword;
    private String password;

    /**
     * 描述
     */
    private String title;

    /**
     * 管理标签
     */
    private String tags;
    /**
     * 扩展字段
     */
    private String extra;

    private ComponentUserRoleInfo componentUserRoleInfo;

    public void valid() {
        CommonUtils.requireNonNull(appName, "appName can't be empty");
        if (StringUtils.containsWhitespace(appName)) {
            throw new PowerJobException("appName can't contains white space!");
        }
        CommonUtils.requireNonNull(password, "password can't be empty");

        // 后续版本强制要求设置 namespace，方便统一管理
        CommonUtils.requireNonNull(namespaceId, "namespace can't be empty");
    }
}
