package tech.powerjob.common.response;

import tech.powerjob.common.PowerSerializable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * The result object returned by the request
 * <p>
 * 低版本由于 Jackson 序列化配置问题，导致无法在此对象上新增任何字段了，否则会报错 com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException: Unrecognized field "code" (class tech.powerjob.common.response.ObjectResultDTO), not marked as ignorable (3 known properties: "data", "success", "message"])
 *  at [Source: (String)"{"success":true,"code":null,"data":2,"message":null}"; line: 1, column: 28] (through reference chain: tech.powerjob.common.response.ObjectResultDTO["code"])
 * <p>
 *  短期内所有的新增字段需求，都通过新对象继承实现
 *
 * @author tjq
 * @since 2020/3/30
 */
@Getter
@Setter
@ToString
public class ResultDTO<T> implements PowerSerializable {

    protected boolean success;
    protected T data;
    protected String message;

    public static <T> ResultDTO<T> success(T data) {
        ResultDTO<T> r = new ResultDTO<>();
        r.success = true;
        r.data = data;
        return r;
    }

    public static <T> ResultDTO<T> failed(String message) {
        ResultDTO<T> r = new ResultDTO<>();
        r.success = false;
        r.message = message;
        return r;
    }

    public static <T> ResultDTO<T> failed(Throwable t) {
        return failed(ExceptionUtils.getStackTrace(t));
    }

}
