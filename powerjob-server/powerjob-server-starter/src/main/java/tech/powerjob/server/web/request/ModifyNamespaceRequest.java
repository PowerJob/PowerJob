package tech.powerjob.server.web.request;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.utils.CommonUtils;

/**
 * ModifyNamespaceRequest
 *
 * @author tjq
 * @since 2023/9/3
 */
@Data
public class ModifyNamespaceRequest {

    private Long id;

    /**
     * 空间唯一标识
     */
    private String code;

    /**
     * 空间名称，比如中文描述（XX部门XX空间）
     */
    private String name;

    private String dept;

    /**
     * 标签，扩展性之王，多值逗号分割
     */
    private String tags;

    private Integer status;

    /**
     * 扩展字段
     */
    private String extra;
    /**
     * 权限表单
     */
    private ComponentUserRoleInfo componentUserRoleInfo;

    public void valid() {
        CommonUtils.requireNonNull(code, "namespace code can't be empty");
        if (StringUtils.containsWhitespace(code)) {
            throw new PowerJobException("namespace code can't contains white space!");
        }
    }
}
