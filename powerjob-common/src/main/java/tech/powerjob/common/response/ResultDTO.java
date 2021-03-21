package tech.powerjob.common.response;

import tech.powerjob.common.PowerSerializable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * The result object returned by the request
 *
 * @author tjq
 * @since 2020/3/30
 */
@Getter
@Setter
@ToString
public class ResultDTO<T> implements PowerSerializable {

    private boolean success;
    private T data;
    private String message;

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
