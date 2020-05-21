---
title: 执行器（Worker）初始化
weight: 2
---

## 基于宿主应用的执行器初始化

> 宿主应用即原有的业务应用，假如需要调度执行的任务与当前业务有较为紧密的联系，建议采取该方式。

首先，添加相关的jar包依赖，最新依赖版本请参考maven中央仓库：[推荐地址](https://search.maven.org/search?q=oh-my-scheduler-worker)&[备用地址](https://mvnrepository.com/search?q=com.github.kfcfans)

```xml
<dependency>
  <groupId>com.github.kfcfans</groupId>
  <artifactId>oh-my-scheduler-worker</artifactId>
  <version>1.2.0</version>
</dependency>
```

其次，填写执行器客户端配置文件`OhMyConfig`，各参数说明如下表所示：

| 属性名称        | 含义                                                         | 默认值                     |
| --------------- | ------------------------------------------------------------ | -------------------------- |
| appName         | 宿主应用名称，需要提前在控制台完成注册                       | 无，必填项，否则启动报错   |
| port            | Worker工作端口                                               | 27777                      |
| serverAddress   | 调度中心（oh-my-scheduler-server）地址列表                   | 无，必填项，否则启动报错   |
| storeStrategy   | 本地存储策略，枚举值磁盘/内存，大型MapReduce等会产生大量Task的任务推荐使用磁盘降低内存压力，否则建议使用内存加速计算 | StoreStrategy.DISK（磁盘） |
| maxResultLength | 每个Task返回结果的默认长度，超长将被截断。过长可能导致网络拥塞 | 8096                       |
| enableTestMode  | 是否启用测试模式，启用后无需Server也能顺利启动OhMyScheduler-Worker，用于处理器本地的单元测试 | false                      |

最后，初始化客户端，完成执行器的启动，代码示例如下：

```java
@Configuration
public class OhMySchedulerConfig {
    @Bean
    public OhMyWorker initOMS() throws Exception {

        // 服务器HTTP地址（端口号为 server.port，而不是 ActorSystem port）
        List<String> serverAddress = Lists.newArrayList("127.0.0.1:7700", "127.0.0.1:7701");

        // 1. 创建配置文件
        OhMyConfig config = new OhMyConfig();
        config.setPort(27777);
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

***

**OhMyScheduler日志单独配置**

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
无论如何，OhMyScheduler-Worker启动时都会打印Banner（如下所示），您可以通过Banner来判断日志配置是否成功（emmm...Markdown显示似乎有点丑，实际上超帅的呢～）：

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





## 基于agent的执行器初始化

> agent是一个没有任何业务逻辑的执行器（其实就是为worker加了一个main方法）。

代码编译方式启动示例：`java -jar oh-my-scheduler-worker-agent-1.2.0.jar -a my-agent`：

```
Usage: OhMyAgent [-hV] -a=<appName> [-e=<storeStrategy>] [-l=<length>]
                 [-p=<port>] [-s=<server>]
OhMyScheduler-Worker代理
  -a, --app=<appName>     worker-agent名称，可通过调度中心控制台创建
  -e, --persistence=<storeStrategy>
                          存储策略，枚举值，DISK 或 MEMORY
  -h, --help              Show this help message and exit.
  -l, --length=<length>   返回值最大长度
  -p, --port=<port>       worker-agent的ActorSystem监听端口，不建议更改
  -s, --server=<server>   调度中心地址，多值英文逗号分隔，格式 IP:Port OR domain
  -V, --version           Print version information and exit.

```

Docker镜像：[Docker Hub](https://hub.docker.com/r/tjqq/oms-agent)