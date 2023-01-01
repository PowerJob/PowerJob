package tech.powerjob.remote.framework.base;

import java.io.IOException;

/**
 * RemotingException
 *
 * @author tjq
 * @since 2022/12/31
 */
public class RemotingException extends RuntimeException {

    public RemotingException(String message) {
        super(message);
    }
}
