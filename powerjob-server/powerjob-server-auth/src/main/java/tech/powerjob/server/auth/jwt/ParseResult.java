package tech.powerjob.server.auth.jwt;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Map;

/**
 * 解析结果
 *
 * @author tjq
 * @since 2024/8/11
 */
@Data
@Accessors(chain = true)
public class ParseResult implements Serializable {

    /**
     * 解析状态
     */
    private Status status;
    /**
     * 解析结果
     */
    private Map<String, Object> result;

    private String msg;

    public enum Status {
        SUCCESS,
        EXPIRED,
        FAILED
    }
}
