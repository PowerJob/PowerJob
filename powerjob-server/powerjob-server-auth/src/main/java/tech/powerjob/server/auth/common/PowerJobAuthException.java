package tech.powerjob.server.auth.common;

import lombok.Getter;
import tech.powerjob.common.enums.ErrorCodes;
import tech.powerjob.common.exception.PowerJobException;

/**
 * 鉴权相关错误
 *
 * @author tjq
 * @since 2024/2/10
 */
@Getter
public class PowerJobAuthException extends PowerJobException {

    public PowerJobAuthException(ErrorCodes errorCode) {
        this(errorCode, null);
    }

    public PowerJobAuthException(ErrorCodes errorCode, String extraMsg) {
        super(errorCode, extraMsg);
    }
}
