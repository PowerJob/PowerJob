package tech.powerjob.remote.akka;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import lombok.extern.slf4j.Slf4j;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.remote.framework.actor.ActorInfo;
import tech.powerjob.remote.framework.actor.HandlerInfo;
import tech.powerjob.remote.framework.base.HandlerLocation;
import tech.powerjob.remote.framework.utils.RemoteUtils;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * 代理用的 actor
 *
 * @author tjq
 * @since 2023/1/6
 */
@Slf4j
public class AkkaProxyActor extends AbstractActor {

    private final Receive receive;
    private final ActorInfo actorInfo;

    public static Props props(ActorInfo actorInfo) {
        return Props.create(AkkaProxyActor.class, () -> new AkkaProxyActor(actorInfo));
    }

    public AkkaProxyActor(ActorInfo actorInfo) {
        this.actorInfo = actorInfo;
        final ReceiveBuilder receiveBuilder = receiveBuilder();
        actorInfo.getHandlerInfos().forEach(handlerInfo -> {
            final HandlerLocation location = handlerInfo.getLocation();
            final Method handlerMethod = handlerInfo.getMethod();
            final Optional<Class<?>> powerSerializeClz = RemoteUtils.findPowerSerialize(handlerMethod.getParameterTypes());
            if (!powerSerializeClz.isPresent()) {
                throw new PowerJobException("build proxy for handler failed due to handler args is not PowerSerialize: " + location);
            }
            final Class<?> bindClz = powerSerializeClz.get();
            receiveBuilder.match(bindClz, req -> onReceiveProcessorReportTaskStatusReq(req, handlerInfo));
        });
        this.receive = receiveBuilder.build();
    }

    @Override
    public Receive createReceive() {
        return receive;
    }

    private <T> void onReceiveProcessorReportTaskStatusReq(T req, HandlerInfo handlerInfo) {

        try {
            final Object ret = handlerInfo.getMethod().invoke(actorInfo.getActor(), req);
            if (ret == null) {
                return;
            }
            if (ret instanceof Optional) {
                if (!((Optional<?>) ret).isPresent()) {
                    return;
                }
            }
            getSender().tell(ret, getSelf());
        } catch (Exception e) {
            log.error("[PowerJob-AKKA] process failed!", e);
        }
    }
}
