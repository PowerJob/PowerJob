# 2020.4.8 第一轮测试
## 测试用例
* MapReduce任务：http://localhost:7700/job/save?appId=1&concurrency=5&executeType=MAP_REDUCE&groupName=null&instanceRetryNum=3&instanceTimeLimit=4545454545&jobDescription=jobDescription&jobName=testJob&jobParams=%7B%22a%22%3A%22b%22%7D&maxInstanceNum=1&processorInfo=com.github.kfcfans.oms.processors.TestMapReduceProcessor&processorType=EMBEDDED_JAVA&status=1&taskRetryNum=3&taskTimeLimit=564465656&timeExpression=0%20*%20*%20*%20*%20%3F%20&timeExpressionType=CRON

## 问题记录
#### 任务执行成功，释放资源失败
第一个任务执行完成后，释放资源阶段（删除本地H2数据库中所有记录）报错，堆栈如下：
```text
2020-04-08 10:09:19 INFO  - [ProcessorTracker-1586311659084] mission complete, ProcessorTracker already destroyed!
2020-04-08 10:09:19 ERROR - [TaskPersistenceService] deleteAllTasks failed, instanceId=1586311659084.
java.lang.InterruptedException: sleep interrupted
	at java.lang.Thread.sleep(Native Method)
	at com.github.kfcfans.common.utils.CommonUtils.executeWithRetry(CommonUtils.java:34)
	at com.github.kfcfans.oms.worker.persistence.TaskPersistenceService.execute(TaskPersistenceService.java:297)
	at com.github.kfcfans.oms.worker.persistence.TaskPersistenceService.deleteAllTasks(TaskPersistenceService.java:269)
	at com.github.kfcfans.oms.worker.core.tracker.task.CommonTaskTracker.destroy(TaskTracker.java:231)
	at com.github.kfcfans.oms.worker.core.tracker.task.CommonTaskTracker$StatusCheckRunnable.innerRun(TaskTracker.java:421)
	at com.github.kfcfans.oms.worker.core.tracker.task.CommonTaskTracker$StatusCheckRunnable.run(TaskTracker.java:467)
	at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511)
	at java.util.concurrent.FutureTask.runAndReset(FutureTask.java:308)
	at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.access$301(ScheduledThreadPoolExecutor.java:180)
	at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:294)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
	at java.lang.Thread.run(Thread.java:748)
2020-04-08 10:09:19 WARN  - [TaskTracker-1586311659084] delete tasks from database failed.
2020-04-08 10:09:19 INFO  - [TaskTracker-1586311659084] TaskTracker has left the world.
```
随后，Server派发下来的第二个任务也无法完成创建，异常堆栈如下：
```text
2020-04-08 10:10:08 ERROR - [TaskPersistenceService] save taskTaskDO{taskId='0', jobId='1', instanceId='1586311804030', taskName='OMS_ROOT_TASK', address='10.37.129.2:2777', status=1, result='null', failedCnt=0, createdTime=1586311808295, lastModifiedTime=1586311808295} failed.
2020-04-08 10:10:08 ERROR - [TaskTracker-1586311804030] create root task failed.
[ERROR] [04/08/2020 10:10:08.511] [oms-akka.actor.internal-dispatcher-20] [akka://oms/user/task_tracker] create root task failed.
java.lang.RuntimeException: create root task failed.
	at com.github.kfcfans.oms.worker.core.tracker.task.CommonTaskTracker.persistenceRootTask(TaskTracker.java:208)
	at com.github.kfcfans.oms.worker.core.tracker.task.CommonTaskTracker.<init>(TaskTracker.java:81)
	at com.github.kfcfans.oms.worker.actors.TaskTrackerActor.lambda$onReceiveServerScheduleJobReq$2(TaskTrackerActor.java:138)
	at java.util.concurrent.ConcurrentHashMap.computeIfAbsent(ConcurrentHashMap.java:1660)
	at com.github.kfcfans.oms.worker.core.tracker.task.TaskTrackerPool.atomicCreateTaskTracker(TaskTrackerPool.java:30)
	at com.github.kfcfans.oms.worker.actors.TaskTrackerActor.onReceiveServerScheduleJobReq(TaskTrackerActor.java:138)
```
***
原因及解决方案：destroy方法调用了scheduledPool.shutdownNow()方法导致调用该方法的线程池被强制关闭，该方法也自然被中断，数据删到一半没删掉，破坏了数据库结构，后面的insert自然也就失败了。

# 2020.4.11 "集群"测试
#### 任务重试机制失效
原因：SQL中的now()函数返回的是Datetime，不能用ing/bigint去接收... 

#### SystemMetric算分问题
问题：java.lang.management.OperatingSystemMXBean#getSystemLoadAverage 不一定能获取CPU当前负载，可能返回负数代表不可用...
解决方案：印度Windows上getSystemLoadAverage()固定返回-1...太坑了...先做个保护性判断继续测试吧...

#### 未知的数组越界问题（可能是数据库性能问题）
问题：秒级Broadcast任务在第四次执行时，当Processor完成执行上报状态时，TaskTracker报错，错误的本质原因是无法从数据库中找到这个task对应的记录...
场景：时间表达式：FIX_DELAY，对应的TaskTracker为FrequentTaskTracker

异常堆栈
```text
2020-04-16 18:05:09 ERROR - [TaskPersistenceService] getTaskStatus failed, instanceId=1586857062542,taskId=4.
java.lang.IndexOutOfBoundsException: Index: 0, Size: 0
	at java.util.LinkedList.checkElementIndex(LinkedList.java:555)
	at java.util.LinkedList.get(LinkedList.java:476)
	at com.github.kfcfans.oms.worker.persistence.TaskPersistenceService.lambda$getTaskStatus$10(TaskPersistenceService.java:214)
	at com.github.kfcfans.common.utils.CommonUtils.executeWithRetry(CommonUtils.java:37)
	at com.github.kfcfans.oms.worker.persistence.TaskPersistenceService.execute(TaskPersistenceService.java:310)
	at com.github.kfcfans.oms.worker.persistence.TaskPersistenceService.getTaskStatus(TaskPersistenceService.java:212)
	at com.github.kfcfans.oms.worker.core.tracker.task.TaskTracker.updateTaskStatus(TaskTracker.java:107)
	at com.github.kfcfans.oms.worker.core.tracker.task.TaskTracker.broadcast(TaskTracker.java:214)
	at com.github.kfcfans.oms.worker.actors.TaskTrackerActor.onReceiveBroadcastTaskPreExecuteFinishedReq(TaskTrackerActor.java:106)
	at akka.japi.pf.UnitCaseStatement.apply(CaseStatements.scala:24)
	at akka.japi.pf.UnitCaseStatement.apply(CaseStatements.scala:20)
	at scala.PartialFunction.applyOrElse(PartialFunction.scala:187)
	at scala.PartialFunction.applyOrElse$(PartialFunction.scala:186)
	at akka.japi.pf.UnitCaseStatement.applyOrElse(CaseStatements.scala:20)
	at scala.PartialFunction$OrElse.applyOrElse(PartialFunction.scala:241)
	at scala.PartialFunction$OrElse.applyOrElse(PartialFunction.scala:242)
	at scala.PartialFunction$OrElse.applyOrElse(PartialFunction.scala:242)
	at scala.PartialFunction$OrElse.applyOrElse(PartialFunction.scala:242)
	at scala.PartialFunction$OrElse.applyOrElse(PartialFunction.scala:242)
	at akka.actor.Actor.aroundReceive(Actor.scala:534)
	at akka.actor.Actor.aroundReceive$(Actor.scala:532)
	at akka.actor.AbstractActor.aroundReceive(AbstractActor.scala:220)
	at akka.actor.ActorCell.receiveMessage(ActorCell.scala:573)
	at akka.actor.ActorCell.invoke(ActorCell.scala:543)
	at akka.dispatch.Mailbox.processMailbox(Mailbox.scala:269)
	at akka.dispatch.Mailbox.run(Mailbox.scala:230)
	at akka.dispatch.Mailbox.exec(Mailbox.scala:242)
	at java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:289)
	at java.util.concurrent.ForkJoinPool$WorkQueue.runTask(ForkJoinPool.java:1056)
	at java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:1692)
	at java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:157)
2020-04-16 18:05:09 WARN  - [TaskTracker-1586857062542] query TaskStatus from DB failed when try to update new TaskStatus(taskId=4,newStatus=6).
```
解决方案：初步怀疑在连续更改时，由于数据库锁的存在导致行不可见（不知道H2具体的特性）。因此，需要保证同一个taskId串行更新 -> synchronize Yes！

# 2020.4.20 1.0.0发布前测试
#### Server & Worker
* 指定机器执行 -> 验证通过
* Map/MapReduce/Standalone/Broadcast/Shell/Python处理器的执行 -> 验证通过
* 超时失败 -> 验证通过
* 破坏测试：指定错误的处理器 -> 发现问题，会造成死锁(TT创建PT，PT创建失败，无法定期汇报心跳，TT长时间未收到PT心跳，认为PT宕机（确实宕机了），无法选择可用的PT再次派发任务，死锁形成，GG斯密达 T_T)。通过确保ProcessorTracker一定能创建成功解决，如果处理器构建失败，之后所有提交的任务直接返回错误。
#### Client
* StopInstance -> success
* FetchInstanceStatus -> success

