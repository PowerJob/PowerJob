package tech.powerjob.remote.framework.engine.impl;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import tech.powerjob.common.OmsConstant;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.remote.framework.actor.*;
import tech.powerjob.remote.framework.base.HandlerLocation;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

/**
 * load all Actor
 *
 * @author tjq
 * @since 2022/12/31
 */
@Slf4j
class ActorFactory {

    static List<ActorInfo> load(List<PowerJobActor> actorList) {

        List<ActorInfo> actorInfos = Lists.newArrayList();

        actorList.forEach(actor -> {
            final Class<? extends PowerJobActor> clz = actor.getClass();
            try {
                final Actor anno = clz.getAnnotation(Actor.class);

                ActorInfo actorInfo = new ActorInfo().setActor(actor).setAnno(anno);
                actorInfo.setHandlerInfos(loadHandlerInfos4Actor(actorInfo));

                actorInfos.add(actorInfo);
            } catch (Throwable t) {
                log.error("[ActorFactory] process Actor[{}] failed!", clz);
                ExceptionUtils.rethrow(t);
            }
        });

        return actorInfos;
    }

    private static List<HandlerInfo> loadHandlerInfos4Actor(ActorInfo actorInfo) {
        List<HandlerInfo> ret = Lists.newArrayList();

        Actor anno = actorInfo.getAnno();
        String rootPath = anno.path();
        Object actor = actorInfo.getActor();

        Method[] declaredMethods = actor.getClass().getDeclaredMethods();
        for (Method handlerMethod: declaredMethods) {
            Handler handlerMethodAnnotation = handlerMethod.getAnnotation(Handler.class);
            if (handlerMethodAnnotation == null) {
                continue;
            }

            HandlerLocation handlerLocation = new HandlerLocation()
                    .setRootPath(rootPath)
                    .setMethodPath(handlerMethodAnnotation.path());

            HandlerInfo handlerInfo = new HandlerInfo()
                    .setAnno(handlerMethodAnnotation)
                    .setMethod(handlerMethod)
                    .setLocation(handlerLocation);
            ret.add(handlerInfo);
        }
        return ret;
    }
}
