package tech.powerjob.server.web.response;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * 基础版本的命名空间
 *
 * @author tjq
 * @since 2023/9/3
 */
@Getter
@Setter
@ToString
public class NamespaceBaseVO implements Serializable {

    private Long id;

    /**
     * 空间唯一标识
     */
    private String code;

    /**
     * 空间名称，比如中文描述（XX部门XX空间）
     */
    private String name;

    private Integer status;
    private String statusStr;

    /**
     * 扩展字段
     */
    private String extra;

    private Date gmtCreate;

    private String gmtCreateStr;

    private Date gmtModified;

    private String gmtModifiedStr;

    private String creator;

    private String modifier;
}
