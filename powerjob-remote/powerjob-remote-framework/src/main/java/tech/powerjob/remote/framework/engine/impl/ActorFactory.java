package tech.powerjob.remote.framework.engine.impl;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import tech.powerjob.common.OmsConstant;
import tech.powerjob.remote.framework.actor.Actor;
import tech.powerjob.remote.framework.actor.ActorInfo;
import tech.powerjob.remote.framework.actor.Handler;
import tech.powerjob.remote.framework.actor.HandlerInfo;
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

    static List<ActorInfo> load() {
        Reflections reflections = new Reflections(OmsConstant.PACKAGE);
        final Set<Class<?>> typesAnnotatedWith = reflections.getTypesAnnotatedWith(Actor.class);

        List<ActorInfo> actorInfos = Lists.newArrayList();
        typesAnnotatedWith.forEach(clz -> {
            try {
                final Actor anno = clz.getAnnotation(Actor.class);
                final Object object = clz.getDeclaredConstructor().newInstance();

                log.info("[ActorFactory] load Actor[clz={},path={}] successfully!", clz, anno.path());

                ActorInfo actorInfo = new ActorInfo().setActor(object).setAnno(anno);
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
        Set<Method> allHandlerMethods = ReflectionUtils.getAllMethods(actor.getClass(), (input -> input != null && input.isAnnotationPresent(Handler.class)));
        allHandlerMethods.forEach(handlerMethod -> {
            Handler handlerMethodAnnotation = handlerMethod.getAnnotation(Handler.class);

            HandlerLocation handlerLocation = new HandlerLocation()
                    .setRootPath(rootPath)
                    .setMethodPath(handlerMethodAnnotation.path());


            HandlerInfo handlerInfo = new HandlerInfo()
                    .setAnno(handlerMethodAnnotation)
                    .setMethod(handlerMethod)
                    .setLocation(handlerLocation);
            ret.add(handlerInfo);
        });
        return ret;
    }
}
