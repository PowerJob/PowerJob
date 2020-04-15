# 简介
### 产品介绍
OhMyScheduler是一个分布式调度平台和分布式计算框架
* 支持CRON、固定频率、固定延迟和API四种定时策略
* 支持单机、广播、**MapReduce**三种执行模式
* 支持任意的水平扩展，性能强劲无上限
* 仅依赖数据库，部署简单，上手容易，开发高效，仅需几行代码即可获得整个集群的分布式计算能力。
* 支持SpringBean、普通Java类（内置/外置）、Shell、Python等处理器（开发中...马上实现）

# 部署
### 环境要求
* 运行环境：JDK8+
* 编译环境：Maven3+
* 数据库：Spring Data JPA支持的关系型数据库理论上都可以（MySQL/Oracle...）

### 项目部署
1. 部署数据库：由于调度Server数据持久化层基于Spring Data Jpa实现，**开发者仅需完成数据库的创建**，即运行SQL`CREATE database if NOT EXISTS oms default character set utf8mb4 collate utf8mb4_unicode_ci;`
2. 部署调度服务器（oh-my-scheduler-server）：修改配置文件（application.properties），按需修改，之后maven打包部署运行一条龙。
3. 部署前端页面（可选，server多实例部署时需要），自行使用[源码](https://github.com/KFCFans/OhMyScheduler-Console)打包部署即可。
4. 被调度任务集成`oh-my-scheduler-worker`依赖，并完成处理器的开发，详细教程见[开发文档](https://github.com/KFCFans/OhMyScheduler/blob/master/others/doc/DevelopmentGuide.md)。

# 开发日志
### 已完成
* 定时调度功能：支持CRON表达式、固定时间间隔、固定频率和API四种方式。
* 任务执行功能：支持单机、广播和MapReduce三种执行方式。
* 高可用与水平扩展：调度服务器可以部署任意数量的节点，不存在调度的性能瓶颈。
* 不怎么美观但可以用的前端界面

### 待开发
* 工作流（任务编排）：当前版本勉强可以用MapReduce代替，不过工作流挺酷的，等框架稳定后进行开发。
* 更多的执行器：当前只支持内置Java执行器，至少需要支持常用的shell、python和外置Java（顺便提供jar包上传下载功能）处理器（这个问题不大，肝就行）。

# 参考
>Alibaba SchedulerX 2.0

* [Akka 框架](https://yq.aliyun.com/articles/709946?spm=a2c4e.11153959.teamhomeleft.67.6a0560c9bZEnZq)：不得不说，akka-remote简化了相当大一部分的网络通讯代码。
* [执行器架构设计](https://yq.aliyun.com/articles/704121?spm=a2c4e.11153959.teamhomeleft.97.371960c9qhB1mB)：这篇文章反而不太认同，感觉我个人的设计更符合Yarn的“架构”。
* [MapReduce模型](https://yq.aliyun.com/articles/706820?spm=a2c4e.11153959.teamhomeleft.83.6a0560c9bZEnZq)：想法很Cool，大数据处理框架都是处理器向数据移动，但对于传统Java应用来说，数据向处理器移动也未尝不可，这样还能使框架的实现变得简单很多。
* [广播执行](https://yq.aliyun.com/articles/716203?spm=a2c4e.11153959.teamhomeleft.40.371960c9qhB1mB)：运行清理日志脚本什么的，也太实用了8～