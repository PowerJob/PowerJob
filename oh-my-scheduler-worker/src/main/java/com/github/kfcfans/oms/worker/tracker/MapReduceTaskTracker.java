package com.github.kfcfans.oms.worker.tracker;

import akka.actor.ActorRef;
import com.github.kfcfans.oms.worker.pojo.model.JobInstanceInfo;
import com.github.kfcfans.oms.worker.pojo.request.WorkerMapTaskRequest;


/**
 * MapReduce 任务使用的 TaskTracker
 *
 * @author tjq
 * @since 2020/3/17
 */
public class MapReduceTaskTracker extends StandaloneTaskTracker {


    public MapReduceTaskTracker(JobInstanceInfo jobInstanceInfo, ActorRef taskTrackerActorRef) {
        super(jobInstanceInfo, taskTrackerActorRef);
    }

    public void newTask(WorkerMapTaskRequest mapRequest) {

    }
}
