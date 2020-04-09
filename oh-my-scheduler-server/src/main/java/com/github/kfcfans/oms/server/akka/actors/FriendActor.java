package com.github.kfcfans.oms.server.akka.actors;

import akka.actor.AbstractActor;
import com.github.kfcfans.common.request.TaskTrackerReportInstanceStatusReq;
import com.github.kfcfans.common.request.WorkerHeartbeat;
import com.github.kfcfans.common.response.AskResponse;
import com.github.kfcfans.oms.server.akka.OhMyServer;
import com.github.kfcfans.oms.server.akka.requests.Ping;
import com.github.kfcfans.oms.server.akka.requests.RedirectServerStopInstanceReq;
import com.github.kfcfans.oms.server.service.ha.WorkerManagerService;
import com.github.kfcfans.oms.server.service.instance.InstanceManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

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
                .match(RedirectServerStopInstanceReq.class, this::onReceiveRedirectServerStopInstanceReq)
                .matchAny(obj -> log.warn("[ServerActor] receive unknown request: {}.", obj))
                .build();
    }

    /**
     * 处理存活检测的请求
     * @param ping 存活检测请求
     */
    private void onReceivePing(Ping ping) {
        AskResponse askResponse = new AskResponse();
        askResponse.setSuccess(true);
        askResponse.setExtra(System.currentTimeMillis() - ping.getCurrentTime());
        getSender().tell(askResponse, getSelf());
    }

    /**
     * 处理停止任务实例的请求
     * @param req 停止运行任务实例
     */
    private void onReceiveRedirectServerStopInstanceReq(RedirectServerStopInstanceReq req) {

        Long instanceId = req.getServerStopInstanceReq().getInstanceId();
        String taskTrackerAddress = InstanceManager.getTaskTrackerAddress(instanceId);

        // 非空，发请求停止任务实例
        if (StringUtils.isNotEmpty(taskTrackerAddress)) {
            OhMyServer.getTaskTrackerActor(taskTrackerAddress).tell(req.getServerStopInstanceReq(), getSelf());
            return;
        }

        // 空，可能刚经历 Server 变更 或 TaskTracker 宕机。先忽略吧，打条日志压压惊
        log.warn("[FriendActor] can't find TaskTracker's address for instance(instanceId={}), so stop instance may fail.", instanceId);
    }
}
