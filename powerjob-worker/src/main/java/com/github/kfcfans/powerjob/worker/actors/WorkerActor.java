package com.github.kfcfans.powerjob.worker.actors;

import akka.actor.AbstractActor;
import com.github.kfcfans.powerjob.common.request.ServerDeployContainerRequest;
import com.github.kfcfans.powerjob.common.request.ServerDestroyContainerRequest;
import com.github.kfcfans.powerjob.worker.container.OmsContainerFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * Worker节点Actor，接受服务器请求
 *
 * @author tjq
 * @since 2020/3/24
 */
@Slf4j
public class WorkerActor extends AbstractActor {

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ServerDeployContainerRequest.class, this::onReceiveServerDeployContainerRequest)
                .match(ServerDestroyContainerRequest.class, this::onReceiveServerDestroyContainerRequest)
                .matchAny(obj -> log.warn("[WorkerActor] receive unknown request: {}.", obj))
                .build();
    }

    private void onReceiveServerDeployContainerRequest(ServerDeployContainerRequest request) {
        OmsContainerFactory.deployContainer(request);
    }

    private void onReceiveServerDestroyContainerRequest(ServerDestroyContainerRequest request) {
        OmsContainerFactory.destroyContainer(request.getContainerId());
    }
}
