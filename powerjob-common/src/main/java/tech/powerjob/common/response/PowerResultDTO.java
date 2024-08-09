package tech.powerjob.common.response;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.exception.ExceptionUtils;
import tech.powerjob.common.enums.ErrorCodes;
import tech.powerjob.common.exception.PowerJobException;

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
        PowerResultDTO<T> f = f(ExceptionUtils.getStackTrace(t));
        f.setCode(ErrorCodes.SYSTEM_UNKNOWN_ERROR.getCode());
        return f;
    }

    public static <T> PowerResultDTO<T> f(PowerJobException pje) {
        PowerResultDTO<T> f = f(pje.getMessage());
        f.setCode(pje.getCode());
        return f;
    }

}
