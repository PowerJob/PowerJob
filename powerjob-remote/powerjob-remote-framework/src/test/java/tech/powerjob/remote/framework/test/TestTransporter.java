package tech.powerjob.remote.framework.test;

import lombok.extern.slf4j.Slf4j;
import tech.powerjob.common.PowerSerializable;
import tech.powerjob.common.response.AskResponse;
import tech.powerjob.remote.framework.base.RemotingException;
import tech.powerjob.remote.framework.base.URL;
import tech.powerjob.remote.framework.transporter.Protocol;
import tech.powerjob.remote.framework.transporter.Transporter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * TestTransporter
 *
 * @author tjq
 * @since 2023/11/16
 */
@Slf4j
public class TestTransporter implements Transporter {
    @Override
    public Protocol getProtocol() {
        return null;
    }

    @Override
    public void tell(URL url, PowerSerializable request) {
        log.info("[TestTransporter] invoke tell, url: {}, request: {}", url, request);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> CompletionStage<T> ask(URL url, PowerSerializable request, Class<T> clz) throws RemotingException {
        log.info("[TestTransporter] invoke ask, url: {}, request: {}", url, request);
        AskResponse askResponse = new AskResponse();
        askResponse.setSuccess(true);
        askResponse.setMessage("FromTestTransporter, force success");
        return (CompletionStage<T>) CompletableFuture.completedFuture(askResponse);
    }
}
