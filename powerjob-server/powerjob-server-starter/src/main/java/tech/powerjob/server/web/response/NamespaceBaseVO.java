package tech.powerjob.server.web.response;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * namespace 基本 VO 对象，用于列表渲染
 *
 * @author tjq
 * @since 2024/2/12
 */
@Getter
@Setter
public class NamespaceBaseVO implements Serializable {

    protected Long id;

    /**
     * 空间唯一标识
     */
    protected String code;

    /**
     * 空间名称，比如中文描述（XX部门XX空间）
     */
    protected String name;

    private String dept;
    private String tags;

    /**
     * 扩展字段
     */
    private String extra;

    private Integer status;
    private String statusStr;

    private Date gmtCreate;

    private String gmtCreateStr;

    private Date gmtModified;

    private String gmtModifiedStr;

    /**
     * 前端名称（拼接 code + name，更容易辨认）
     */
    protected String showName;

    public void genShowName() {
        showName = String.format("%s(%s)", name, code);
    }
}
