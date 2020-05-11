# STEP2: 处理器开发
>OhMyScheduler支持Python、Shell和Java处理器，前两种处理器为脚本处理器，功能简单，在控制台直接配置即可，本章不再赘述。开发项目内置的Java处理器，宿主应用需要添加`oh-my-scheduler-worker`依赖，并实现指定接口或抽象类的Java类。

* 最新依赖版本请参考Maven中央仓库：[推荐地址](https://search.maven.org/search?q=com.github.kfcfans)&[备用地址](https://mvnrepository.com/search?q=com.github.kfcfans)。

```xml
<dependency>
  <groupId>com.github.kfcfans</groupId>
  <artifactId>oh-my-scheduler-worker</artifactId>
  <version>${oms.worker.latest.version}</version>
</dependency>
```

## 处理器开发示例
>更多示例代码请见项目：oh-my-scheduler-worker-samples

#### 单机处理器
>单机执行的策略下，server会在所有可用worker中选取健康度最佳的机器进行执行。单机执行任务需要实现接口：`com.github.kfcfans.oms.worker.core.processor.sdk.BasicProcessor`，代码示例如下：

```java
// 支持 SpringBean 的形式
@Component
public class BasicProcessorDemo implements BasicProcessor {

    @Override
    public ProcessResult process(TaskContext context) throws Exception {

        // 在线日志功能，可以直接在控制台查看任务日志，非常便捷
        OmsLogger omsLogger = context.getOmsLogger();
        omsLogger.info("BasicProcessorDemo start to process, current JobParams is {}.", context.getJobParams());
        
        // TaskContext为任务的上下文信息，包含了在控制台录入的任务元数据，常用字段为
        // jobParams（任务参数，在控制台录入），instanceParams（任务实例参数，通过 OpenAPI 触发的任务实例才可能存在该参数）

        // 进行实际处理...

        // 返回结果，该结果会被持久化到数据库，在前端页面直接查看，极为方便
        return new ProcessResult(true, "result is xxx");
    }
}
```

#### 广播执行处理器
>广播执行的策略下，所有机器都会被调度执行该任务。为了便于资源的准备和释放，广播处理器在`BasicProcessor`的基础上额外增加了`preProcess`和`postProcess`方法，分别在整个集群开始之前/结束之后**选一台机器**执行相关方法。代码示例如下：

```java
public class BroadcastProcessorDemo extends BroadcastProcessor {

    @Override
    public ProcessResult preProcess(TaskContext taskContext) throws Exception {
        // 预执行，会在所有 worker 执行 process 方法前调用
        return new ProcessResult(true, "init success");
    }

    @Override
    public ProcessResult process(TaskContext context) throws Exception {
        // 撰写整个worker集群都会执行的代码逻辑
        return new ProcessResult(true, "release resource success");
    }

    @Override
    public ProcessResult postProcess(TaskContext taskContext, List<TaskResult> taskResults) throws Exception {

        // taskResults 存储了所有worker执行的结果（包括preProcess）

        // 收尾，会在所有 worker 执行完毕 process 方法后调用，该结果将作为最终的执行结果在
        return new ProcessResult(true, "process success");
    }

}
```

#### MapReduce处理器
>MapReduce是最复杂也是最强大的一种执行器，它允许开发者完成任务的拆分，将子任务派发到集群中其他Worker执行，是执行大批量处理任务的不二之选！实现MapReduce处理器需要继承`MapReduceProcessor`类，具体用法如下示例代码所示。

```java
public class MapReduceProcessorDemo extends MapReduceProcessor {

    @Override
    public ProcessResult process(TaskContext context) throws Exception {
        // 判断是否为根任务
        if (isRootTask()) {

            // 构造子任务
            List<SubTask> subTaskList = Lists.newLinkedList();

            /*
             * 子任务的构造由开发者自己定义
             * eg. 现在需要从文件中读取100W个ID，并处理数据库中这些ID对应的数据，那么步骤如下：
             * 1. 根任务（RootTask）读取文件，流式拉取100W个ID，并按1000个一批的大小组装成子任务进行派发
             * 2. 非根任务获取子任务，完成业务逻辑的处理
             */

            // 调用 map 方法，派发子任务
            return map(subTaskList, "DATA_PROCESS_TASK");
        }

        // 非子任务，可根据 subTask 的类型 或 TaskName 来判断分支
        if (context.getSubTask() instanceof SubTask) {
            // 执行子任务，注：子任务人可以 map 产生新的子任务，可以构建任意级的 MapReduce 处理器
            return new ProcessResult(true, "PROCESS_SUB_TASK_SUCCESS");
        }

        return new ProcessResult(false, "UNKNOWN_BUG");
    }

    @Override
    public ProcessResult reduce(TaskContext taskContext, List<TaskResult> taskResults) {

        // 所有 Task 执行结束后，reduce 将会被执行
        // taskResults 保存了所有子任务的执行结果

        // 用法举例，统计执行结果
        AtomicLong successCnt = new AtomicLong(0);
        taskResults.forEach(tr -> {
            if (tr.isSuccess()) {
                successCnt.incrementAndGet();
            }
        });
        return new ProcessResult(true, "success task num:" + successCnt.get());
    }

    // 自定义的子任务
    private static class SubTask {
        private Long siteId;
        private List<Long> idList;
    }
}
```

## 处理器上下文（TaskContext）属性说明
|属性名称|意义/用法|
|----|----|
|instanceId|任务实例ID，全局唯一，开发者一般无需关心此参数|
|subInstanceId|子任务实例ID，秒级任务使用，开发者一般无需关心此参数|
|taskId|采用链式命名法的ID，在某个任务实例内唯一，开发者一般无需关心此参数|
|taskName|task名称，Map/MapReduce任务的子任务的值为开发者指定，否则为系统默认值，开发者一般无需关心此参数|
|jobParams|任务参数，其值等同于控制台录入的**任务参数**，常用！|
|instanceParams|任务实例参数，其值等同于使用OpenAPI运行任务实例时传递的参数，常用！|
|maxRetryTimes|Task的最大重试次数|
|currentRetryTimes|Task的当前重试次数，和maxRetryTimes联合起来可以判断当前是否为该Task的最后一次运行机会|
|subTask|子Task，Map/MapReduce处理器专属，开发者调用map方法时传递的子任务列表中的某一个|
|omsLogger|在线日志，用法同Slf4J，记录的日志可以直接通过控制台查看，非常便捷和强大！不过使用过程中需要注意频率，可能对Server造成巨大的压力|
