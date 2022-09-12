package tech.powerjob.server.core.handler.impl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import tech.powerjob.common.RemoteConstant;
import tech.powerjob.server.remote.transport.starter.AkkaStarter;
import tech.powerjob.server.remote.transport.starter.VertXStarter;

import javax.annotation.PostConstruct;

/**
 * 初始化器
 *
 * @author tjq
 * @since 2022/9/11
 */
@Component
@ConditionalOnExpression("'${execution.env}'!='test'")
public class Initializer {

    @PostConstruct
    public void initHandler() {
        // init akka
        AkkaStarter.actorSystem.actorOf(WorkerRequestAkkaHandler.defaultProps(), RemoteConstant.SERVER_ACTOR_NAME);
        // init vert.x
        VertXStarter.vertx.deployVerticle(new WorkerRequestHttpHandler());
    }
}
