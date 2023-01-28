package tech.powerjob.server.remote.server.redirector;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import tech.powerjob.common.RemoteConstant;
import tech.powerjob.common.enums.Protocol;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.common.response.AskResponse;
import tech.powerjob.remote.framework.base.URL;
import tech.powerjob.server.persistence.remote.model.AppInfoDO;
import tech.powerjob.server.persistence.remote.repository.AppInfoRepository;
import tech.powerjob.server.remote.transporter.impl.ServerURLFactory;
import tech.powerjob.server.remote.transporter.TransportService;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * 指定服务器运行切面
 *
 * @author tjq
 * @since 12/13/20
 */
@Slf4j
@Aspect
@Component
@Order(0)
@RequiredArgsConstructor
public class DesignateServerAspect {

    private final TransportService transportService;
    private final AppInfoRepository appInfoRepository;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
            throw new PowerJobException("can't find appId in params for:" + signature);
        }

        // 获取执行机器
        AppInfoDO appInfo = appInfoRepository.findById(appId).orElseThrow(() -> new PowerJobException("can't find app info"));
        String targetServer = appInfo.getCurrentServer();

        // 目标IP为空，本地执行
        if (StringUtils.isEmpty(targetServer)) {
            return point.proceed();
        }

        // 目标IP与本地符合则本地执行
        if (Objects.equals(targetServer, transportService.defaultProtocol().getAddress())) {
            return point.proceed();
        }

        log.info("[DesignateServerAspect] the method[{}] should execute in server[{}], so this request will be redirect to remote server!", signature.toShortString(), targetServer);
        // 转发请求，远程执行后返回结果
        RemoteProcessReq remoteProcessReq = new RemoteProcessReq()
                .setClassName(className)
                .setMethodName(methodName)
                .setParameterTypes(parameterTypes)
                .setArgs(args);

        final URL friendUrl = ServerURLFactory.process2Friend(targetServer);

        CompletionStage<AskResponse> askCS = transportService.ask(Protocol.HTTP.name(), friendUrl, remoteProcessReq, AskResponse.class);
        AskResponse askResponse = askCS.toCompletableFuture().get(RemoteConstant.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        if (!askResponse.isSuccess()) {
            throw new PowerJobException("remote process failed: " + askResponse.getMessage());
        }

        // 考虑范型情况
        Method method = methodSignature.getMethod();
        JavaType returnType = getMethodReturnJavaType(method);

        return OBJECT_MAPPER.readValue(askResponse.getData(), returnType);
    }


    private static JavaType getMethodReturnJavaType(Method method) {
        Type type = method.getGenericReturnType();
        return getJavaType(type);
    }

    private static JavaType getJavaType(Type type) {
        if (type instanceof ParameterizedType) {
            Type[] actualTypeArguments = ((ParameterizedType)type).getActualTypeArguments();
            Class<?> rowClass = (Class<?>) ((ParameterizedType)type).getRawType();
            JavaType[] javaTypes = new JavaType[actualTypeArguments.length];
            for (int i = 0; i < actualTypeArguments.length; i++) {
                //泛型也可能带有泛型，递归处理
                javaTypes[i] = getJavaType(actualTypeArguments[i]);
            }
            return TypeFactory.defaultInstance().constructParametricType(rowClass, javaTypes);
        } else {
            return TypeFactory.defaultInstance().constructParametricType((Class<?>) type, new JavaType[0]);
        }
    }
}
