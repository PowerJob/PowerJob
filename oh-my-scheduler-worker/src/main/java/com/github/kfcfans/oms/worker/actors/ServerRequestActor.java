package com.github.kfcfans.oms.worker.actors;

import akka.actor.AbstractActor;
import com.github.kfcfans.oms.worker.pojo.request.ServerScheduleJobReq;
import lombok.extern.slf4j.Slf4j;

/**
 * 处理来自服务器的请求
 * 请求链：server -> taskTracker -> worker
 *
 * @author tjq
 * @since 2020/3/17
 */
@Slf4j
public class ServerRequestActor extends AbstractActor {

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ServerScheduleJobReq.class, this::onReceiveServerScheduleJobReq)
                .matchAny(obj -> log.warn("[ServerRequestActor] receive unknown request: {}.", obj))
                .build();
    }

    private void onReceiveServerScheduleJobReq(ServerScheduleJobReq req) {
        // 接受到任务，创建 TaskTracker
    }


}
