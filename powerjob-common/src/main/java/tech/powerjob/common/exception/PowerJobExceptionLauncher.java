package tech.powerjob.common.exception;


import tech.powerjob.common.enums.ErrorCodes;

/**
 * PowerJobExceptionLauncher
 *
 * @author tjq
 * @since 2024/11/22
 */
public class PowerJobExceptionLauncher {

    public PowerJobExceptionLauncher(ErrorCodes errorCode, String message) {
        throw new PowerJobException(errorCode, message);
    }
}
