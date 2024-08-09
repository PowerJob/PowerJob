package tech.powerjob.common.response;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * 新的 Result，带状态码
 *
 * @author 程序帕鲁
 * @since 2024/2/19
 */
@Getter
@Setter
public class PowerResultDTO<T> extends ResultDTO<T> {

    private String code;

    public static <T> PowerResultDTO<T> s(T data) {
        PowerResultDTO<T> r = new PowerResultDTO<>();
        r.success = true;
        r.data = data;
        return r;
    }

    public static <T> PowerResultDTO<T> f(String message) {
        PowerResultDTO<T> r = new PowerResultDTO<>();
        r.success = false;
        r.message = message;
        return r;
    }

    public static <T> PowerResultDTO<T> f(Throwable t) {
        return f(ExceptionUtils.getStackTrace(t));
    }

}
