package tech.powerjob.server.extension.alarm;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * 报警目标
 *
 * @author tjq
 * @since 2023/7/16
 */
@Data
@Accessors(chain = true)
public class AlarmTarget implements Serializable {

    private String name;
    /**
     * 手机号
     */
    private String phone;
    /**
     * 邮箱地址
     */
    private String email;
    /**
     * webHook
     */
    private String webHook;
    /**
     * 扩展字段
     */
    private String extra;

    private Map<String, Objects> attributes;
}
