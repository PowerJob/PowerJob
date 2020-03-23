package com.github.kfcfans.oms.worker.actors;

import akka.actor.AbstractActor;
import com.github.kfcfans.common.request.ServerScheduleJobReq;
import lombok.extern.slf4j.Slf4j;

/**
 * worker的master节点，处理来自server的jobInstance请求和来自worker的task请求
 *
 * @author tjq
 * @since 2020/3/17
 */
@Slf4j
public class TaskTrackerActor extends AbstractActor {

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
