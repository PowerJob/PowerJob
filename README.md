# 简介
### 产品介绍
OhMyScheduler是一个分布式调度平台和分布式计算框架，具有以下特性：
* 支持CRON、固定频率、固定延迟和API四种调度策略。
* 支持单机、广播、**MapReduce**三种执行模式。
* 支持任意的水平扩展，性能强劲无上限。
* 具有强大的故障转移与恢复能力，只要保证集群可用节点数足够，任务就能顺利完成。
* 仅依赖数据库，部署简单，上手容易，开发高效，仅需几行代码即可获得整个集群的分布式计算能力。
* 支持SpringBean、普通Java类（内置/外置）、Shell、Python等处理器。

# 部署
### 环境要求
* 运行环境：JDK8+
* 编译环境：Maven3+
* 数据库：Spring Data JPA支持的关系型数据库理论上都可以（MySQL/Oracle/MS SQLServer...）

### 依赖坐标
>最新依赖版本请参考Maven中央仓库：[推荐地址](https://search.maven.org/search?q=com.github.kfcfans)&[备用地址](https://mvnrepository.com/search?q=com.github.kfcfans)。

Worker依赖包（需要接入分布式计算能力的应用需要依赖该jar包）
```xml
<dependency>
  <groupId>com.github.kfcfans</groupId>
  <artifactId>oh-my-scheduler-worker</artifactId>
  <version>${oms.worker.latest.version}</version>
</dependency>
```
Client依赖包（需要接入OpenAPI的应用需要依赖该Jar包，使用代码调用控制台功能）
```xml
<dependency>
  <groupId>com.github.kfcfans</groupId>
  <artifactId>oh-my-scheduler-client</artifactId>
  <version>${oms.client.latest.version}</version>
</dependency>
```

### 项目部署
1. 部署数据库：由于调度Server数据持久化层基于Spring Data Jpa实现，**开发者仅需完成数据库的创建**，即运行SQL`CREATE database if NOT EXISTS oms default character set utf8mb4 collate utf8mb4_unicode_ci;`
2. 部署调度服务器（oh-my-scheduler-server）：修改配置文件（application.properties），按需修改，之后maven打包部署运行一条龙。
3. 部署前端页面（可选，server多实例部署时需要，单Server已经继承在SpringBoot中），自行使用[源码](https://github.com/KFCFans/OhMyScheduler-Console)打包部署即可。
4. 被调度任务集成`oh-my-scheduler-worker`依赖，并完成处理器的开发，详细教程见[开发文档](https://github.com/KFCFans/OhMyScheduler/blob/master/others/doc/DevelopmentGuide.md)。

# 开发日志
### 已完成
* 定时调度功能：支持CRON表达式、固定时间间隔、固定频率和API四种方式。
* 任务执行功能：支持单机、广播和MapReduce三种执行方式。
* 执行处理器：支持SpringBean、普通Java对象、Shell脚本、Python脚本的执行
* 高可用与水平扩展：调度服务器可以部署任意数量的节点，不存在调度的性能瓶颈。
* 不怎么美观但可以用的前端界面
* OpenAPI：通过OpenAPI可以允许开发者在自己的应用上对OhMyScheduler进行二次开发，比如开发自己的定时调度策略，通过API的调度模式触发任务执行。

### 下阶段目标
* 工作流（任务编排）：当前版本勉强可以用MapReduce代替，不过工作流挺酷的，等框架稳定后进行开发。
* [应用级别资源管理和任务优先级](https://yq.aliyun.com/articles/753141?spm=a2c4e.11153959.teamhomeleft.1.696d60c9vt9lLx)：没有机器资源时，进入排队队列。不过我觉得SchedulerX的方案不太行，SchedulerX无抢占，一旦低优先级任务开始运行，那么只能等他执行完成才能开始高优先级任务，这明显不合理。可是考虑抢占的话又要多考虑很多东西...先放在TODO列表吧。
* 保护性判断（这个太繁琐了且意义不大，毕竟面向开发者，大家不会乱填参数对不对～）

# 参考
>Alibaba SchedulerX 2.0

* [Akka 框架](https://yq.aliyun.com/articles/709946?spm=a2c4e.11153959.teamhomeleft.67.6a0560c9bZEnZq)：不得不说，akka-remote简化了相当大一部分的网络通讯代码。
* [执行器架构设计](https://yq.aliyun.com/articles/704121?spm=a2c4e.11153959.teamhomeleft.97.371960c9qhB1mB)：这篇文章反而不太认同，感觉我个人的设计更符合Yarn的“架构”。
* [MapReduce模型](https://yq.aliyun.com/articles/706820?spm=a2c4e.11153959.teamhomeleft.83.6a0560c9bZEnZq)：想法很Cool，大数据处理框架都是处理器向数据移动，但对于传统Java应用来说，数据向处理器移动也未尝不可，这样还能使框架的实现变得简单很多。
* [广播执行](https://yq.aliyun.com/articles/716203?spm=a2c4e.11153959.teamhomeleft.40.371960c9qhB1mB)：运行清理日志脚本什么的，也太实用了8～

# 后记
* 产品永久开源（Apache License, Version 2.0），免费使用，且目前开发者@KFCFans有充足的时间进行项目维护和提供无偿技术支持（All In 了解一下），欢迎各位试用！
* 欢迎共同参与本项目的贡献，PR和Issue都大大滴欢迎（求求了）～
* 觉得还不错的话，可以点个Star支持一下哦～ =￣ω￣=
* 联系肥宅兄@KFCFans -> `tengjiqi@gmail.com`