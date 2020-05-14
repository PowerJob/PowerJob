# STEP4: OpenAPI

## 快速开始
>OpenAPI允许开发者通过接口来完成手工的操作，让系统整体变得更加灵活，启用OpenAPI需要依赖`oh-my-scheduler-client`库。

最新依赖版本请参考Maven中央仓库：[推荐地址](https://search.maven.org/search?q=com.github.kfcfans)&[备用地址](https://mvnrepository.com/search?q=com.github.kfcfans)。

```xml
<dependency>
  <groupId>com.github.kfcfans</groupId>
  <artifactId>oh-my-scheduler-client</artifactId>
  <version>${oms.client.latest.version}</version>
</dependency>
```

### 简单示例

```text
// 初始化 client，需要server地址和应用名称作为参数
OhMyClient ohMyClient = new OhMyClient("127.0.0.1:7700", "oms-test");
// 调用相关的API
ohMyClient.stopInstance(1586855173043L)
```

## 功能列表
#### 创建/修改任务
接口签名：`ResultDTO<Long> saveJob(ClientJobInfo newJobInfo)`
入参：任务信息（详细说明见下表）
返回值：任务ID

|属性|说明|
|----|----|
|jobId|任务ID，可选，null代表创建任务，否则填写需要修改的任务ID|
|jobName|任务名称|
|jobDescription|任务描述|
|jobParams|任务参数，Processor#process方法入参`TaskContext`对象的jobParams字段|
|timeExpressionType|时间表达式类型，枚举值|
|timeExpression|时间表达式，填写类型由timeExpressionType决定，比如CRON需要填写CRON表达式|
|executeType|执行类型，枚举值|
|processorType|处理器类型，枚举值|
|processorInfo|处理器参数，填写类型由processorType决定，如Java处理器需要填写全限定类名，如：com.github.kfcfans.oms.processors.demo.MapReduceProcessorDemo|
|maxInstanceNum|最大实例数，该任务同时执行的数量（任务和实例就像是类和对象的关系，任务被调度执行后被称为实例）|
|concurrency|单机线程并发数，表示该实例执行过程中每个Worker使用的线程数量|
|instanceTimeLimit|任务实例运行时间限制，0代表无任何限制，超时会被打断并判定为执行失败|
|instanceRetryNum|任务实例重试次数，整个任务失败时重试，代价大，不推荐使用|
|taskRetryNum|Task重试次数，每个子Task失败后单独重试，代价小，推荐使用|
|minCpuCores|最小可用CPU核心数，CPU可用核心数小于该值的Worker将不会执行该任务，0代表无任何限制|
|minMemorySpace|最小内存大小（GB），可用内存小于该值的Worker将不会执行该任务，0代表无任何限制|
|minDiskSpace|最小磁盘大小（GB），可用磁盘空间小于该值的Worker将不会执行该任务，0代表无任何限制|
|designatedWorkers|指定机器执行，设置该参数后只有列表中的机器允许执行该任务，0代表无任何限制|
|maxWorkerCount|最大执行机器数量，限定调动执行的机器数量，空代表无限制|
|notifyUserIds|接收报警的用户ID列表|
|enable|是否启用该任务，未启用的任务不会被调度|

#### 禁用某个任务
接口签名：`ResultDTO<Void> disableJob(Long jobId)`
入参：任务ID
返回值：无

#### 删除某个任务
接口签名：`ResultDTO<Void> deleteJob(Long jobId)`
入参：任务ID
返回值：无

#### 立即运行某个任务
接口签名：`ResultDTO<Long> runJob(Long jobId, String instanceParams)`
入参：任务ID + **任务实例参数**（Processor#process方法入参`TaskContext`对象的instanceParams字段）
返回值：对应的任务实例ID(instanceId)