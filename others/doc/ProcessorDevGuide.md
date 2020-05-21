# STEP2: 处理器开发
>OhMyScheduler支持Python、Shell和Java处理器，前两种处理器为脚本处理器，功能简单，在控制台直接配置即可，本章主要介绍内置于Java项目的处理器开发。

## 宿主应用接入
#### 添加依赖
* 最新依赖版本请参考Maven中央仓库：[推荐地址](https://search.maven.org/search?q=oh-my-scheduler-worker)&[备用地址](https://mvnrepository.com/search?q=com.github.kfcfans)。

```xml
<dependency>
  <groupId>com.github.kfcfans</groupId>
  <artifactId>oh-my-scheduler-worker</artifactId>
  <version>${oms.worker.latest.version}</version>
</dependency>
```
#### 初始化客户端：OhMyScheduler-Worker
> 客户端启动类为`OhMyWorker`，需要设置配置文件`OhMyConfig`并启动，以下为配置文件说明和配置示例。

OhMyConfig属性说明：

|属性名称|含义|默认值|
|----|----|----|
|appName|宿主应用名称，需要提前在控制台完成注册|无，必填项，否则启动报错|
|serverAddress|服务器（OhMyScheduler-Server）地址列表|无，必填项，否则启动报错|
|storeStrategy|本地存储策略，枚举值磁盘/内存，大型MapReduce等会产生大量Task的任务推荐使用磁盘降低内存压力，否则建议使用内存加速计算|StoreStrategy.DISK（磁盘）|
|maxResultLength|每个Task返回结果的默认长度，超长将被截断。过长可能导致网络拥塞|8096|
|enableTestMode|是否启用测试模式，启用后无需Server也能顺利启动OhMyScheduler-Worker，用于处理器本地的单元测试|false|

OhMyWorker启动配置（Spring/SpringBoot模式）：
```java
@Configuration
public class OhMySchedulerConfig {
    @Bean
    public OhMyWorker initOMS() throws Exception {

        // 服务器HTTP地址（端口号为 server.port，而不是 ActorSystem port）
        List<String> serverAddress = Lists.newArrayList("127.0.0.1:7700", "127.0.0.1:7701");

        // 1. 创建配置文件
        OhMyConfig config = new OhMyConfig();
        config.setAppName("oms-test");
        config.setServerAddress(serverAddress);
        // 如果没有大型 Map/MapReduce 的需求，建议使用内存来加速计算
        // 为了本地模拟多个实例，只能使用 MEMORY 启动（文件只能由一个应用占有）
        config.setStoreStrategy(StoreStrategy.MEMORY);

        // 2. 创建 Worker 对象，设置配置文件
        OhMyWorker ohMyWorker = new OhMyWorker();
        ohMyWorker.setConfig(config);
        return ohMyWorker;
    }
}
```
非Spring应用程序在创建`OhMyWorker`对象后手动调用`ohMyWorker.init()`方法完成初始化即可。

### 配置日志
目前，OhMyScheduler-Worker并没有实现自己的LogFactory（如果有需求的话请提ISSUE，可以考虑实现），原因如下：
1. OhMyScheduler-Worker的日志基于`Slf4J`输出，即采用了基于门面设计模式的日志框架，宿主应用无论如何都可以搭起Slf4J与实际的日志框架这座桥梁。
2. 减轻了部分开发工作量，不再需要实现自己的LogFactory（虽然不怎么难就是了...）。

为此，为了顺利且友好地输出日志，请在日志配置文件（logback.xml/log4j2.xml/...）中为`OhMyScheduler-Worker`单独进行日志配置，比如（logback示例）：
```xml
<appender name="OMS_WORKER_APPENDER" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>${LOG_PATH}/oms-worker.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <FileNamePattern>${LOG_PATH}/oms-worker.%d{yyyy-MM-dd}.log</FileNamePattern>
        <MaxHistory>7</MaxHistory>
    </rollingPolicy>
    <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
        <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n</pattern>
        <charset>UTF-8</charset>
    </encoder>
    <append>true</append>
</appender>

<logger name="com.github.kfcfans.oms.worker" level="INFO" additivity="false">
    <appender-ref ref="OMS_WORKER_APPENDER" />
</logger>
```
无论如何，OhMyScheduler-Worker启动时都会打印Banner（如下所示），您可以通过Banner来判断日志配置是否成功：
```text
   ███████   ██      ████     ████           ████████         ██                   ██          ██
  ██░░░░░██ ░██     ░██░██   ██░██  ██   ██ ██░░░░░░         ░██                  ░██         ░██               
 ██     ░░██░██     ░██░░██ ██ ░██ ░░██ ██ ░██         █████ ░██       █████      ░██ ██   ██ ░██  █████  ██████
░██      ░██░██████ ░██ ░░███  ░██  ░░███  ░█████████ ██░░░██░██████  ██░░░██  ██████░██  ░██ ░██ ██░░░██░░██░░█
░██      ░██░██░░░██░██  ░░█   ░██   ░██   ░░░░░░░░██░██  ░░ ░██░░░██░███████ ██░░░██░██  ░██ ░██░███████ ░██ ░ 
░░██     ██ ░██  ░██░██   ░    ░██   ██           ░██░██   ██░██  ░██░██░░░░ ░██  ░██░██  ░██ ░██░██░░░░  ░██   
 ░░███████  ░██  ░██░██        ░██  ██      ████████ ░░█████ ░██  ░██░░██████░░██████░░██████ ███░░██████░███   
  ░░░░░░░   ░░   ░░ ░░         ░░  ░░      ░░░░░░░░   ░░░░░  ░░   ░░  ░░░░░░  ░░░░░░  ░░░░░░ ░░░  ░░░░░░ ░░░
```

## 处理器开发
>开发者需要根据实际需求实现`BasicProcessor`接口或继承`BroadcastProcessor`、`MapProcessor`或`MapReduceProcessor`抽象类实现处理器的开发。处理器的核心方法为`ProcessResult process(TaskContext context)`，以下为详细说明：

ProcessResult为处理返回结果，包含`success`和`msg`两个属性。

TaskContext为处理的入参，包含了本次处理的上下文信息，具体属性如下：

|属性名称|意义/用法|
|----|----|
|jobId|任务ID，开发者一般无需关心此参数|
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

#### 单机处理器
>单机执行的策略下，server会在所有可用worker中选取健康度最佳的机器进行执行。单机执行任务需要实现接口：`com.github.kfcfans.oms.worker.core.processor.sdk.BasicProcessor`，代码示例如下：

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

#### 广播执行处理器
>广播执行的策略下，所有机器都会被调度执行该任务。为了便于资源的准备和释放，广播处理器在`BasicProcessor`的基础上额外增加了`preProcess`和`postProcess`方法，分别在整个集群开始之前/结束之后**选一台机器**执行相关方法。代码示例如下：

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

#### MapReduce处理器
>MapReduce是最复杂也是最强大的一种执行器，它允许开发者完成任务的拆分，将子任务派发到集群中其他Worker执行，是执行大批量处理任务的不二之选！实现MapReduce处理器需要继承`MapReduceProcessor`类，具体用法如下示例代码所示。

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

更多示例请见：[oh-my-scheduler-worker-samples](../../oh-my-scheduler-worker-samples)

