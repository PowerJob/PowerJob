package com.github.kfcfans.oms.worker.actors;

import akka.actor.AbstractActor;

/**
 * 普通计算节点，处理来自 JobTracker 的请求
 *
 * @author tjq
 * @since 2020/3/17
 */
public class WorkerActor extends AbstractActor {
    @Override
    public Receive createReceive() {
        return null;
    }
}
