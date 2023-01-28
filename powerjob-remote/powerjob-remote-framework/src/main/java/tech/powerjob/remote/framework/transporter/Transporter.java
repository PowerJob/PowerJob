package tech.powerjob.remote.framework.transporter;

import tech.powerjob.common.PowerSerializable;
import tech.powerjob.remote.framework.base.RemotingException;
import tech.powerjob.remote.framework.base.URL;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

/**
 * 通讯器，封装与远程服务端交互逻辑
 *
 * @author tjq
 * @since 2022/12/31
 */
public interface Transporter {

    /**
     * Protocol
     * @return return protocol
     */
    Protocol getProtocol();

    /**
     *send message
     * @param url url
     * @param request request
     */
    void tell(URL url, PowerSerializable request);

    /**
     * ask by request
     * @param url url
     * @param request request
     * @param clz response type
     * @return CompletionStage
     * @throws RemotingException remote exception
     */
    <T> CompletionStage<T> ask(URL url, PowerSerializable request, Class<T> clz) throws RemotingException;
}
