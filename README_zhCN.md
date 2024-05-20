# [English](./README.md) | 简体中文

<p align="center">
🏮PowerJob 全体成员祝大家龙年腾飞，新的一年身体健康，万事如意，阖家欢乐，幸福安康！🏮
</p>

<p align="center">
<img src="https://raw.githubusercontent.com/KFCFans/PowerJob/master/others/images/logo.png" alt="PowerJob" title="PowerJob" width="557"/>
</p>

<p align="center">
<a href="https://github.com/PowerJob/PowerJob/actions"><img src="https://github.com/PowerJob/PowerJob/workflows/Java%20CI%20with%20Maven/badge.svg?branch=master" alt="actions"></a>
<a href="https://search.maven.org/search?q=tech.powerjob"><img alt="Maven Central" src="https://img.shields.io/maven-central/v/tech.powerjob/powerjob-worker"></a>
<a href="https://github.com/PowerJob/PowerJob/releases"><img alt="GitHub release (latest SemVer)" src="https://img.shields.io/github/v/release/kfcfans/powerjob?color=%23E59866"></a>
<a href="https://github.com/PowerJob/PowerJob/blob/master/LICENSE"><img src="https://img.shields.io/github/license/KFCFans/PowerJob" alt="LICENSE"></a>
</p>

PowerJob（原OhMyScheduler）是全新一代分布式调度与计算框架，能让您轻松完成作业的调度与繁杂任务的分布式计算。
# 简介
### 主要特性
* 使用简单：提供前端Web界面，允许开发者可视化地完成调度任务的管理（增、删、改、查）、任务运行状态监控和运行日志查看等功能。
* 定时策略完善：支持CRON表达式、固定频率、固定延迟和API四种定时调度策略。
* 执行模式丰富：支持单机、广播、Map、MapReduce四种执行模式，其中Map/MapReduce处理器能使开发者寥寥数行代码便获得集群分布式计算的能力。
* DAG工作流支持：支持在线配置任务依赖关系，可视化得对任务进行编排，同时还支持上下游任务间的数据传递
* 执行器支持广泛：支持Spring Bean、内置/外置Java类、Shell、Python等处理器，应用范围广。
* 运维便捷：支持在线日志功能，执行器产生的日志可以在前端控制台页面实时显示，降低debug成本，极大地提高开发效率。
* 依赖精简：最小仅依赖关系型数据库（MySQL/Oracle/MS SQLServer...）。
* 高可用&高性能：调度服务器经过精心设计，一改其他调度框架基于数据库锁的策略，实现了无锁化调度。部署多个调度服务器可以同时实现高可用和性能的提升（支持无限的水平扩展）。
* 故障转移与恢复：任务执行失败后，可根据配置的重试策略完成重试，只要执行器集群有足够的计算节点，任务就能顺利完成。

### 适用场景
* 有定时执行需求的业务场景：如每天凌晨全量同步数据、生成业务报表等。
* 有需要全部机器一同执行的业务场景：如使用广播执行模式清理集群日志。
* 有需要分布式处理的业务场景：比如需要更新一大批数据，单机执行耗时非常长，可以使用Map/MapReduce处理器完成任务的分发，调动整个集群加速计算。
* 有需要**延迟执行**某些任务的业务场景：比如订单过期处理等。

### 设计目标
PowerJob 的设计目标为企业级的分布式任务调度平台，即成为公司内部的**任务调度中间件**。整个公司统一部署调度中心 powerjob-server，旗下所有业务线应用只需要依赖 `powerjob-worker` 即可接入调度中心获取任务调度与分布式计算能力。

### 在线试用
* [点击查看试用说明和教程](https://www.yuque.com/powerjob/guidence/trial)

### 同类产品对比
|                | QuartZ                   | xxl-job                                  | SchedulerX 2.0                                    | PowerJob                                                |
| -------------- | ------------------------ | ---------------------------------------- | ------------------------------------------------- | ------------------------------------------------------------ |
| 定时类型       | CRON                     | CRON                                     | CRON、固定频率、固定延迟、OpenAPI                 | **CRON、固定频率、固定延迟、OpenAPI**                        |
| 任务类型       | 内置Java                 | 内置Java、GLUE Java、Shell、Python等脚本 | 内置Java、外置Java（FatJar）、Shell、Python等脚本 | **内置Java、外置Java（容器）、Shell、Python等脚本**          |
| 分布式计算     | 无                       | 静态分片                                 | MapReduce动态分片                                 | **MapReduce动态分片**                                        |
| 在线任务治理   | 不支持                   | 支持                                     | 支持                                              | **支持**                                                     |
| 日志白屏化     | 不支持                   | 支持                                     | 不支持                                            | **支持**                                                     |
| 调度方式及性能 | 基于数据库锁，有性能瓶颈 | 基于数据库锁，有性能瓶颈                 | 不详                                              | **无锁化设计，性能强劲无上限**                               |
| 报警监控       | 无                       | 邮件                                     | 短信                                              | **WebHook、邮件、钉钉与自定义扩展**                             |
| 系统依赖       | JDBC支持的关系型数据库（MySQL、Oracle...）                    | MySQL                                    | 人民币        | **任意Spring Data Jpa支持的关系型数据库（MySQL、Oracle...）** |
| DAG工作流      | 不支持                   | 不支持                                   | 支持                                              | **支持**                                   |


# 官方文档
**[中文文档](https://www.yuque.com/powerjob/guidence/intro)**

**[Docs](https://www.yuque.com/powerjob/en/introduce)**

# 接入登记
[点击进行接入登记，为 PowerJob 的发展贡献自己的力量！](https://github.com/PowerJob/PowerJob/issues/6)

ღ( ´・ᴗ・\` )ღ 感谢以下接入用户的大力支持 ღ( ´・ᴗ・\` )ღ

<p align="center">
<img src="https://raw.githubusercontent.com/KFCFans/PowerJob/master/others/images/user.png" alt="PowerJob User" title="PowerJob User"/>
</p>

# 其他
* 开源许可证：Apache License, Version 2.0
* 欢迎共同参与本项目的贡献，PR和Issue都大大滴欢迎（求求了）～
* 觉得还不错的话，可以点个Star支持一下哦～ =￣ω￣=
* 联系方式@KFCFans -> `tengjiqi@gmail.com`
* 用户交流QQ群（因广告信息泛滥，加群需要验证，请认真填写申请原因）：
  * 一群（已满）：487453839
  * 二群：834937813