package tech.powerjob.server.remote.server;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.routing.DefaultResizer;
import akka.routing.RoundRobinPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import tech.powerjob.common.response.AskResponse;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.server.remote.server.election.Ping;
import tech.powerjob.server.remote.server.redirector.RemoteProcessReq;
import tech.powerjob.server.remote.server.redirector.RemoteRequestProcessor;
import tech.powerjob.server.remote.transport.TransportService;

/**
 * 处理朋友们的信息（处理服务器与服务器之间的通讯）
 *
 * @author tjq
 * @since 2020/4/9
 */
@Slf4j
public class FriendRequestHandler extends AbstractActor {


    public static Props defaultProps() {
        return Props.create(FriendRequestHandler.class)
                .withDispatcher("akka.friend-request-actor-dispatcher")
                .withRouter(
                        new RoundRobinPool(Runtime.getRuntime().availableProcessors() * 4)
                                .withResizer(new DefaultResizer(
                                        Runtime.getRuntime().availableProcessors() * 4,
                                        Runtime.getRuntime().availableProcessors() * 10,
                                        1,
                                        0.2d,
                                        0.3d,
                                        0.1d,
                                        10
                                ))
                );
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Ping.class, this::onReceivePing)
                .match(RemoteProcessReq.class, this::onReceiveRemoteProcessReq)
                .matchAny(obj -> log.warn("[FriendActor] receive unknown request: {}.", obj))
                .build();
    }


    @Override
    public void preStart() throws Exception {
        super.preStart();
        log.debug("[FriendRequestHandler]init FriendRequestActor");
    }


    @Override
    public void postStop() throws Exception {
        super.postStop();
        log.debug("[FriendRequestHandler]stop FriendRequestActor");
    }

    /**
     * 处理存活检测的请求
     */
    private void onReceivePing(Ping ping) {
        getSender().tell(AskResponse.succeed(TransportService.getAllAddress()), getSelf());
    }

    private void onReceiveRemoteProcessReq(RemoteProcessReq req) {

        AskResponse response = new AskResponse();
        response.setSuccess(true);
        try {
            response.setData(JsonUtils.toBytes(RemoteRequestProcessor.processRemoteRequest(req)));
        } catch (Throwable t) {
            log.error("[FriendActor] process remote request[{}] failed!", req, t);
            response.setSuccess(false);
            response.setMessage(ExceptionUtils.getMessage(t));
        }
        getSender().tell(response, getSelf());
    }
}
