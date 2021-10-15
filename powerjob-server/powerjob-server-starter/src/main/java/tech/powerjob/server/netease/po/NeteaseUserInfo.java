package tech.powerjob.server.netease.po;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author Echo009
 * @since 2021/10/15
 */
@Data
@Accessors(chain = true)
public class NeteaseUserInfo {
    /**
     * 用户名
     */
    @JSONField(name = "sub")
    private String userName;
    /**
     * 用户邮箱地址，可能为空
     */
    @JSONField(name = "email")
    private String email;
    /**
     * 用户中文姓名，可能为空
     */
    @JSONField(name = "fullname")
    private String fullName;
}
