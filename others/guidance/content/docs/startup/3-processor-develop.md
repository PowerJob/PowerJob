---
title: 处理器开发
weight: 3
---

## 处理器概述

> OhMyScheduler当前支持Shell、Python等脚本处理器和Java处理器。脚本处理器只需要开发者完成脚本的编写（xxx.sh / xxx.py），在控制台填入脚本内容即可，本章不再赘述。本章将重点阐述Java处理器开发方法与使用技巧。

* Java处理器可根据**代码所处位置**划分为内置Java处理器和容器Java处理器，前者直接集成在宿主应用（也就是接入本系统的业务应用）中，一般用来处理业务需求；后者可以在一个独立的轻量级的Java工程中开发，通过**容器技术**（详见容器章节）被worker集群热加载，提供Java的“脚本能力”，一般用于处理灵活多变的需求。
* Java处理器可根据**对象创建者**划分为SpringBean处理器和普通Java对象处理器，前者由Spring IOC容器完成处理器的创建和初始化，后者则有OhMyScheduler维护其状态。如果宿主应用支持Spring，**强烈建议使用SpringBean处理器**，开发者仅需要将Processor注册进Spring IOC容器（一个`@Component`注解或一句`bean`配置）。
* Java处理器可根据**功能**划分为单机处理器、广播处理器、Map处理器和MapReduce处理器。
  * 单机处理器（`BasicProcessor`）对应了单机任务，即某个任务的某次运行只会有某一台机器的某一个线程参与运算。
  * 广播处理器（`BroadcastProcessor`）对应了广播任务，即某个任务的某次运行会**调动集群内所有机器参与运算**。
  * Map处理器（`MapProcessor`）对应了Map任务，即某个任务在运行过程中，**允许产生子任务并分发到其他机器进行运算**。
  * MapReduce处理器（`MapReduceProcessor`）对应了MapReduce任务，在Map任务的基础上，**增加了所有任务结束后的汇总统计**。

## 核心方法：process

任意Java处理器都需要实现处理的核心方法，其接口签名如下：

```java
ProcessResult process(TaskContext context) throws Exception;
```

方法入参`TaskContext`，包含了本次处理的上下文信息，具体属性如下：

| 属性名称          | 意义/用法                                                    |
| ----------------- | ------------------------------------------------------------ |
| jobId             | 任务ID，开发者一般无需关心此参数                             |
| instanceId        | 任务实例ID，全局唯一，开发者一般无需关心此参数               |
| subInstanceId     | 子任务实例ID，秒级任务使用，开发者一般无需关心此参数         |
| taskId            | 采用链式命名法的ID，在某个任务实例内唯一，开发者一般无需关心此参数 |
| taskName          | task名称，Map/MapReduce任务的子任务的值为开发者指定，否则为系统默认值，开发者一般无需关心此参数 |
| jobParams         | 任务参数，其值等同于控制台录入的**任务参数**，常用！         |
| instanceParams    | 任务实例参数，其值等同于使用OpenAPI运行任务实例时传递的参数，常用！ |
| maxRetryTimes     | Task的最大重试次数                                           |
| currentRetryTimes | Task的当前重试次数，和maxRetryTimes联合起来可以判断当前是否为该Task的最后一次运行机会 |
| subTask           | 子Task，Map/MapReduce处理器专属，开发者调用map方法时传递的子任务列表中的某一个 |
| omsLogger         | 在线日志，用法同Slf4J，记录的日志可以直接通过控制台查看，非常便捷和强大！不过使用过程中需要注意频率，可能对Server造成巨大的压力 |

方法的返回值为`ProcessResult`，代表了本次Task执行的结果，包含`success`和`msg`两个属性，分别用于传递Task是否执行成功和Task需要返回的信息。

## 单机处理器：BasicProcessor

单机执行的策略下，server会在所有可用worker中选取健康度最佳的机器进行执行。单机执行任务需要实现接口：`BasicProcessor`，代码示例如下：

```java
// 支持 SpringBean 的形式
@Component
public class BasicProcessorDemo implements BasicProcessor {

    @Resource
    private MysteryService mysteryService;

    @Override
    public ProcessResult process(TaskContext context) throws Exception {

        // 在线日志功能，可以直接在控制台查看任务日志，非常便捷
        OmsLogger omsLogger = context.getOmsLogger();
        omsLogger.info("BasicProcessorDemo start to process, current JobParams is {}.", context.getJobParams());
        
        // TaskContext为任务的上下文信息，包含了在控制台录入的任务元数据，常用字段为
        // jobParams（任务参数，在控制台录入），instanceParams（任务实例参数，通过 OpenAPI 触发的任务实例才可能存在该参数）

        // 进行实际处理...
        mysteryService.hasaki();

        // 返回结果，该结果会被持久化到数据库，在前端页面直接查看，极为方便
        return new ProcessResult(true, "result is xxx");
    }
}
```

## 广播处理器：BroadcastProcessor

广播执行的策略下，所有机器都会被调度执行该任务。为了便于资源的准备和释放，广播处理器在`BasicProcessor`的基础上额外增加了`preProcess`和`postProcess`方法，分别在整个集群开始之前/结束之后**选一台机器**执行相关方法。代码示例如下：

```java
@Component
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

        // 收尾，会在所有 worker 执行完毕 process 方法后调用，该结果将作为最终的执行结果
        return new ProcessResult(true, "process success");
    }
}
```

## 并行处理器：MapReduceProcessor

MapReduce是最复杂也是最强大的一种执行器，它允许开发者完成任务的拆分，将子任务派发到集群中其他Worker执行，是执行大批量处理任务的不二之选！实现MapReduce处理器需要继承`MapReduceProcessor`类，具体用法如下示例代码所示：

```java
@Component
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
        // 该结果将作为任务最终的执行结果
        return new ProcessResult(true, "success task num:" + successCnt.get());
    }

    // 自定义的子任务
    private static class SubTask {
        private Long siteId;
        private List<Long> idList;
    }
}
```

注：Map处理器相当于MapReduce处理器的阉割版本（阉割了`reduce`方法），此处不再单独举例。

## 最佳实践：MapReduce实现静态分片

虽然说这有点傻鸡焉用牛刀的感觉，不过既然目前市场上同类产品都处于静态分片的阶段，我也就在这里给大家举个例子吧～

```java
@Component
public class StaticSliceProcessor extends MapReduceProcessor {

    @Override
    public ProcessResult process(TaskContext context) throws Exception {
        OmsLogger omsLogger = context.getOmsLogger();
        
        // root task 负责分发任务
        if (isRootTask()) {
            // 从控制台传递分片参数，架设格式为KV：1=a&2=b&3=c
            String jobParams = context.getJobParams();
            Map<String, String> paramsMap = Splitter.on("&").withKeyValueSeparator("=").split(jobParams);

            List<SubTask> subTasks = Lists.newLinkedList();
            paramsMap.forEach((k, v) -> subTasks.add(new SubTask(Integer.parseInt(k), v)));
            return map(subTasks, "SLICE_TASK");
        }

        Object subTask = context.getSubTask();
        if (subTask instanceof SubTask) {
            // 实际处理
            // 当然，如果觉得 subTask 还是很大，也可以继续分发哦
            
            return new ProcessResult(true, "subTask:" + ((SubTask) subTask).getIndex() + " process successfully");
        }
        return new ProcessResult(false, "UNKNOWN BUG");
    }

    @Override
    public ProcessResult reduce(TaskContext context, List<TaskResult> taskResults) {
        // 按需求做一些统计工作... 不需要的话，直接使用 Map 处理器即可
        return new ProcessResult(true, "xxxx");
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class SubTask {
        private int index;
        private String params;
    }
}
```

## 最佳实践：MapReduce多级分发处理

利用MapReduce实现 Root -> A -> B/C -> Reduce）的DAG 工作流。

```java
@Component
public class DAGSimulationProcessor extends MapReduceProcessor {

    @Override
    public ProcessResult process(TaskContext context) throws Exception {

        if (isRootTask()) {
            // L1. 执行根任务

            // 执行完毕后产生子任务 A，需要传递的参数可以作为 TaskA 的属性进行传递
            TaskA taskA = new TaskA();
            return map(Lists.newArrayList(taskA), "LEVEL1_TASK_A");
        }

        if (context.getSubTask() instanceof TaskA) {
            // L2. 执行A任务

            // 执行完成后产生子任务 B，C（并行执行）
            TaskB taskB = new TaskB();
            TaskC taskC = new TaskC();
            return map(Lists.newArrayList(taskB, taskC), "LEVEL2_TASK_BC");
        }

        if (context.getSubTask() instanceof TaskB) {
            // L3. 执行B任务
            return new ProcessResult(true, "xxx");
        }
        if (context.getSubTask() instanceof TaskC) {
            // L3. 执行C任务
            return new ProcessResult(true, "xxx");
        }

        return new ProcessResult(false, "UNKNOWN_TYPE_OF_SUB_TASK");
    }

    @Override
    public ProcessResult reduce(TaskContext context, List<TaskResult> taskResults) {
        // L4. 执行最终 Reduce 任务，taskResults保存了之前所有任务的结果
        taskResults.forEach(taskResult -> {
            // do something...
        });
        return new ProcessResult(true, "reduce success");
    }

    private static class TaskA {
    }
    private static class TaskB {
    }
    private static class TaskC {
    }
}
```

## 更多示例

没看够？更多示例请见：[oh-my-scheduler-worker-samples](https://github.com/KFCFans/OhMyScheduler/tree/master/oh-my-scheduler-worker-samples)

