package tech.powerjob.remote.framework.engine.impl;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import tech.powerjob.remote.framework.actor.Actor;
import tech.powerjob.remote.framework.actor.ActorInfo;
import tech.powerjob.remote.framework.actor.Handler;
import tech.powerjob.remote.framework.actor.HandlerInfo;
import tech.powerjob.remote.framework.base.HandlerLocation;

import java.lang.reflect.Method;
import java.util.List;

/**
 * load all Actor
 *
 * @author tjq
 * @since 2022/12/31
 */
@Slf4j
class ActorFactory {

    static List<ActorInfo> load(List<Object> actorList) {

        List<ActorInfo> actorInfos = Lists.newArrayList();

        actorList.forEach(actor -> {
            final Class<?> clz = actor.getClass();
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

        findHandlerMethod(rootPath, actor.getClass(), ret);
        return ret;
    }

    private static void findHandlerMethod(String rootPath, Class<?> clz, List<HandlerInfo> result) {
        Method[] declaredMethods = clz.getDeclaredMethods();
        for (Method handlerMethod: declaredMethods) {
            Handler handlerMethodAnnotation = handlerMethod.getAnnotation(Handler.class);
            if (handlerMethodAnnotation == null) {
                continue;
            }

            HandlerLocation handlerLocation = new HandlerLocation()
                    .setRootPath(suitPath(rootPath))
                    .setMethodPath(suitPath(handlerMethodAnnotation.path()));

            HandlerInfo handlerInfo = new HandlerInfo()
                    .setAnno(handlerMethodAnnotation)
                    .setMethod(handlerMethod)
                    .setLocation(handlerLocation);
            result.add(handlerInfo);
        }

        // 递归处理父类
        final Class<?> superclass = clz.getSuperclass();
        if (superclass != null) {
            findHandlerMethod(rootPath, superclass, result);
        }
    }

    static String suitPath(String path) {
        if (path.startsWith("/")) {
            return path.replaceFirst("/", "");
        }
        return path;
    }
}
