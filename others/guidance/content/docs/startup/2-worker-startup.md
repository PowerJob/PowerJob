---
title: 执行器（Worker）初始化
weight: 2
---

## 基于宿主应用的执行器初始化

> 宿主应用即原有的业务应用，假如需要调度执行的任务与当前业务有较为紧密的联系，建议采取该方式。

> OhMyScheduler当前支持Shell、Python等脚本处理器和Java处理器。脚本处理器只需要开发者完成脚本的编写（xxx.sh / xxx.py），在控制台填入脚本内容即可，本章不再赘述。本章将重点阐述Java处理器开发方法与使用技巧。

* Java处理器可根据**代码所处位置**划分为内置Java处理器和容器Java处理器，前者直接集成在宿主应用（也就是接入本系统的业务应用）中，一般用来处理业务需求；后者可以在一个独立的轻量级的Java工程中开发，通过**容器技术**（详见容器章节）被worker集群热加载，提供Java的“脚本能力”，一般用于处理灵活多变的需求。
* Java处理器可根据**对象创建者**划分为SpringBean处理器和普通Java对象处理器，前者由Spring IOC容器完成处理器的创建和初始化，后者则有OhMyScheduler维护其状态。如果宿主应用支持Spring，**强烈建议使用SpringBean处理器**，开发者仅需要将Processor注册进Spring IOC容器（一个`@Component`注解或一句`bean`配置）。
* Java处理器可根据**功能**划分为单机处理器、广播处理器、Map处理器和MapReduce处理器。
  * 单机处理器对应了单机任务，即某个任务的某次运行只会有某一台机器的某一个线程参与运算。
  * 广播处理器对应了广播任务，即某个任务的某次运行会**调动集群内所有机器参与运算**。
  * Map处理器对应了Map任务，即某个任务在运行过程中，**允许产生子任务并分发到其他机器进行运算**。
  * MapReduce处理器对应了MapReduce任务，在Map任务的基础上，**增加了所有任务结束后的汇总统计**。

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

#### 基于agent的执行器初始化

> agent是一个没有任何业务逻辑的执行器（其实就是为worker加了一个main方法）。

代码编译方式启动示例：`java -jar oh-my-scheduler-worker-agent-1.2.0.jar -a my-agent`：

```
Usage: OhMyAgent [-hV] -a=<appName> [-l=<length>] [-p=<storeStrategy>]
                 [-s=<server>]
OhMyScheduler-Worker代理
  -a, --app=<appName>     worker-agent名称，可通过调度中心控制台创建
  -h, --help              Show this help message and exit.
  -l, --length=<length>   返回值最大长度
  -p, --persistence=<storeStrategy>
                          存储策略，枚举值，DISK 或 MEMORY
  -s, --server=<server>   调度中心地址，多值英文逗号分隔，格式 IP:Port OR domain
  -V, --version           Print version information and exit.

```

Docker镜像：[Docker Hub](https://hub.docker.com/r/tjqq/oms-agent)