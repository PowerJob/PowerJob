package com.github.kfcfans.powerjob.server.common.redirect;

import akka.pattern.Patterns;
import com.alibaba.fastjson.JSONObject;
import com.github.kfcfans.powerjob.common.PowerJobException;
import com.github.kfcfans.powerjob.common.RemoteConstant;
import com.github.kfcfans.powerjob.common.response.AskResponse;
import com.github.kfcfans.powerjob.server.akka.OhMyServer;
import com.github.kfcfans.powerjob.server.akka.requests.RemoteProcessReq;
import com.github.kfcfans.powerjob.server.persistence.core.model.AppInfoDO;
import com.github.kfcfans.powerjob.server.persistence.core.repository.AppInfoRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/**
 * 执行服务器运行切面
 *
 * @author tjq
 * @since 12/13/20
 */
@Slf4j
@Aspect
@Component
public class DesignateServerAspect {

    @Resource
    private AppInfoRepository appInfoRepository;

    @Around(value = "@annotation(designateServer))")
    public Object execute(ProceedingJoinPoint point, DesignateServer designateServer) throws Throwable {

        // 参数
        Object[] args = point.getArgs();
        // 方法名
        String methodName = point.getSignature().getName();
        // 类名
        String className = point.getSignature().getDeclaringTypeName();
        Signature signature = point.getSignature();
        // 方法签名
        MethodSignature methodSignature = (MethodSignature) signature;
        String[] parameterNames = methodSignature.getParameterNames();
        String[] parameterTypes = Arrays.stream(methodSignature.getParameterTypes()).map(Class::getName).toArray(String[]::new);

        Long appId = null;
        for (int i = 0; i < parameterNames.length; i++) {
            if (StringUtils.equals(parameterNames[i], designateServer.appIdParameterName())) {
                appId = Long.parseLong(String.valueOf(args[i]));
                break;
            }
        }

        if (appId == null) {
            throw new PowerJobException("can't find appId in params for:" + signature.toString());
        }

        // 获取执行机器
        AppInfoDO appInfo = appInfoRepository.findById(appId).orElseThrow(() -> new PowerJobException("can't find app info"));
        String targetServer = appInfo.getCurrentServer();

        // 目标IP与本地符合则本地执行
        if (Objects.equals(targetServer, OhMyServer.getActorSystemAddress())) {
            return point.proceed();
        }

        log.info("[DesignateServerAspect] the method[{}] should execute in server[{}], so this request will be redirect to remote server!", signature.toShortString(), targetServer);
        // 转发请求，远程执行后返回结果
        RemoteProcessReq remoteProcessReq = new RemoteProcessReq()
                .setClassName(className)
                .setMethodName(methodName)
                .setParameterTypes(parameterTypes)
                .setArgs(args);

        CompletionStage<Object> askCS = Patterns.ask(OhMyServer.getFriendActor(targetServer), remoteProcessReq, Duration.ofMillis(RemoteConstant.DEFAULT_TIMEOUT_MS));
        AskResponse askResponse = (AskResponse) askCS.toCompletableFuture().get();

        if (!askResponse.isSuccess()) {
            throw new PowerJobException("remote process failed: " + askResponse.getMessage());
        }
        return JSONObject.parseObject(askResponse.getData(), methodSignature.getReturnType());
    }
}
