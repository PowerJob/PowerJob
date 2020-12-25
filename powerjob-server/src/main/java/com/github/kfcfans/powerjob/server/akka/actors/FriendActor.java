package com.github.kfcfans.powerjob.server.akka.actors;

import akka.actor.AbstractActor;
import com.alibaba.fastjson.JSONObject;
import com.github.kfcfans.powerjob.common.model.SystemMetrics;
import com.github.kfcfans.powerjob.common.response.AskResponse;
import com.github.kfcfans.powerjob.server.akka.requests.FriendQueryWorkerClusterStatusReq;
import com.github.kfcfans.powerjob.server.akka.requests.Ping;
import com.github.kfcfans.powerjob.server.akka.requests.RemoteProcessReq;
import com.github.kfcfans.powerjob.server.common.utils.SpringUtils;
import com.github.kfcfans.powerjob.server.service.ha.WorkerManagerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 处理朋友们的信息（处理服务器与服务器之间的通讯）
 *
 * @author tjq
 * @since 2020/4/9
 */
@Slf4j
public class FriendActor extends AbstractActor {
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Ping.class, this::onReceivePing)
                .match(RemoteProcessReq.class, this::onReceiveRemoteProcessReq)
                .match(FriendQueryWorkerClusterStatusReq.class, this::onReceiveFriendQueryWorkerClusterStatusReq)
                .matchAny(obj -> log.warn("[FriendActor] receive unknown request: {}.", obj))
                .build();
    }

    /**
     * 处理存活检测的请求
     */
    private void onReceivePing(Ping ping) {
        getSender().tell(AskResponse.succeed(System.currentTimeMillis() - ping.getCurrentTime()), getSelf());
    }

    /**
     * 处理查询Worker节点的请求
     */
    private void onReceiveFriendQueryWorkerClusterStatusReq(FriendQueryWorkerClusterStatusReq req) {
        Map<String, SystemMetrics> workerInfo = WorkerManagerService.getActiveWorkerInfo(req.getAppId());
        AskResponse askResponse = AskResponse.succeed(workerInfo);
        getSender().tell(askResponse, getSelf());
    }

    private void onReceiveRemoteProcessReq(RemoteProcessReq req) {

        AskResponse response = new AskResponse();
        response.setSuccess(true);
        try {

            Object[] args = req.getArgs();
            String[] parameterTypes = req.getParameterTypes();
            Class<?>[] parameters = new Class[parameterTypes.length];

            for (int i = 0; i < parameterTypes.length; i++) {
                parameters[i] = Class.forName(parameterTypes[i]);
                Object arg = args[i];
                if (arg != null) {
                    args[i] = JSONObject.parseObject(JSONObject.toJSONBytes(arg), parameters[i]);
                }
            }

            Class<?> clz = Class.forName(req.getClassName());

            Object bean = SpringUtils.getBean(clz);
            Method method = ReflectionUtils.findMethod(clz, req.getMethodName(), parameters);

            assert method != null;
            Object invokeResult = ReflectionUtils.invokeMethod(method, bean, args);

            response.setData(JSONObject.toJSONBytes(invokeResult));

        } catch (Throwable t) {
            log.error("[FriendActor] process remote request[{}] failed!", req, t);
            response.setSuccess(false);
            response.setMessage(ExceptionUtils.getMessage(t));
        }
        getSender().tell(response, getSelf());
    }
}
