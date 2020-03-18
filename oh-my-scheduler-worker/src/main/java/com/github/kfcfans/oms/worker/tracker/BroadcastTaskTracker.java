package com.github.kfcfans.oms.worker.tracker;

import akka.actor.ActorRef;
import com.github.kfcfans.oms.worker.pojo.model.JobInstanceInfo;

/**
 * 广播任务使用的 TaskTracker
 *
 * @author tjq
 * @since 2020/3/17
 */
public class BroadcastTaskTracker extends TaskTracker {


    public BroadcastTaskTracker(JobInstanceInfo jobInstanceInfo, ActorRef taskTrackerActorRef) {
        super(jobInstanceInfo, taskTrackerActorRef);
    }

    @Override
    public void dispatch() {

    }
}
