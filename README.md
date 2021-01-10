English | [简体中文](./README_zhCN.md)

<p align="center">
<img src="https://raw.githubusercontent.com/KFCFans/PowerJob/master/others/images/logo.png" alt="PowerJob" title="PowerJob" width="557"/>
</p>

<p align="center">
<a href="https://github.com/PowerJob/PowerJob/actions"><img src="https://github.com/PowerJob/PowerJob/workflows/Java%20CI%20with%20Maven/badge.svg?branch=master" alt="actions"></a>
<a href="https://search.maven.org/search?q=com.github.kfcfans"><img alt="Maven Central" src="https://img.shields.io/maven-central/v/com.github.kfcfans/powerjob-worker"></a>
<a href="https://github.com/PowerJob/PowerJob/releases"><img alt="GitHub release (latest SemVer)" src="https://img.shields.io/github/v/release/kfcfans/powerjob?color=%23E59866"></a>
<a href="https://github.com/PowerJob/PowerJob/blob/master/LICENSE"><img src="https://img.shields.io/github/license/KFCFans/PowerJob" alt="LICENSE"></a>
</p>

- Have you ever wondered how cron jobs could be organized orderly? 
- Have you ever felt upset about tasks that carry with complex dependencies?
- Have you ever felt helpless when scheduling tasks suddenly terminated without any warning?
- Have you ever felt depressed when batches of business tasks need to be processed in a distributed manner?

Well, PowerJob is there for you, it is the choice of a new generation. It is a powerful, business-oriented scheduling framework that provides distributed computing ability. Based on Akka architecture, it makes everything with scheduling easier. Just with several steps, PowerJob could be deployed and work for you!

# Introduction

### Features
-   Simple to use: PowerJob provides a friendly front-end Web that allows developers to visually manage tasks, monitor tasks, and view logs online.
-   Complete timing strategy:  PowerJob supports four different scheduling strategies, including CRON expression, fixed frequency timing, fixed delay timing as well as the Open API.
-   Various execution modes: PowerJob supports four execution modes: stand-alone, broadcast, Map, and MapReduce. **It's worth mentioning the Map and MapReduce modes. With several lines of codes, developers could take full advantage of PowerJob's distributed computing ability**.
-   Complete workflow support: PowerJob supports DAG(Directed acyclic graph) based online task configuration. Developers could arrange tasks on the console, while data could be transferred among tasks on the flow.
-   Extensive executor support: PowerJob supports multiple processors, including Spring Beans, ordinary Java objects, Shell, Python and so on.
-   Simple in dependency: PowerJob aims to be simple in dependency. The only dependency is merely database (MySQL / Oracle / MS SQLServer ...), with MongoDB being the extra dependency for storing large log files.
-   High availability and performance: Unlike traditional job-scheduling frameworks that rely on database locks, PowerJob server is lock-free. PowerJob supports unlimited horizontal expansion. It's easy to achieve high availability and performance by deploying as many PowerJob server instances as you need.
-   Quick failover and recovery support: Whenever any task failed, PowerJob server would retry according to the configured strategy. As long as there were enough nodes in the cluster, the failed tasks could execute successfully finally.

### Applicable scenes

-   Scenarios with timed tasks: such as full synchronization of data at midnight, generating business reports at desired time.
-   Scenarios that require all machines to run tasks simultaneously: such as log cleanup.
-   Scenarios that require distributed processing: For example, a large amount of data requires updating, while the stand-alone execution takes quite a lot of time. The Map/MapReduce mode could be applied in which the workers would join the cluster for PowerJob server to dispatch, to speed up the time-consuming process, therefore improving the computing ability of the whole cluster.
-   **Scenarios with delayed tasks**: For instance, disposal of overdue orders.

### Design goals

PowerJob aims to be an enterprise scheduling middleware. By deploying PowerJob-server as the scheduling center,
all the applications could gain scheduling and distributed computing ability relying on PowerJob-worker.

### Online trial

Trial address: [Online Trial Address](http://try.powerjob.tech/)  
Application name: powerjob-agent-test  
Application password: 123

### Comparison with similar products

|                                    | QuartZ                                                    | PowerJob                                                |
| ---------------------------------- | --------------------------------------------------------- | ------------------------------------------------------------ |
| Timing type                        | CRON                                                      | **CRON, fixed frequency, fixed delay, OpenAPI**                  |
| Task type                          | Built-in Java                                             | **Built-in Java, external Java (JVM Container), Shell, Python and other scripts** |
| Distributed strategy               | Unsupported                                               | **MapReduce dynamic sharding**                                   |
| Online task management             | Unsupported                                               | **Supported**                                                  |
| Online logging                     | Unsupported                                               | **Supported**                                                      |
| Scheduling methods and performance | Based on database lock, there is a performance bottleneck | **Lock-free design, high performance without upper limit**   |
| Alarm monitoring                   | Unsupported                                               | **Email, WebHook, DingTalk. An interface is provided for customization.** |
| System dependence                  | Any relational database (MySQL, Oracle ...) supported by JDBC      | **Any relational database (MySQL, Oracle ...) supported by Spring Data Jpa** |
| workflow                           | Unsupported                                               | **Supported**               |
 
# Document
**[Docs](https://www.yuque.com/powerjob/en/introduce)**

**[中文文档](https://www.yuque.com/powerjob/product)**

# User Registration
[Click to register as PowerJob user and contribute to PowerJob!](https://github.com/PowerJob/PowerJob/issues/6)  
ღ( ´・ᴗ・\` )ღ Many thanks to the following registered users. ღ( ´・ᴗ・\` )ღ
<p style="text-align: center">
<img src="https://raw.githubusercontent.com/KFCFans/PowerJob/master/others/images/user.png" alt="PowerJob User" title="PowerJob User"/>
</p>


# Others
-   Welcome to the Gitter Community: [LINK](https://gitter.im/PowerJob/community)
-   PowerJob is permanently open source software(Apache License, Version 2.0), please feel free to try, deploy and put into production!
-   Welcome to contribute to PowerJob, both Pull Requests and Issues are precious. 
-   Please STAR PowerJob if it is valuable. ~ = ￣ω￣ =
-   Do you need any help or want to propose suggestions? Please raise Github issues or contact the Author @KFCFans-> `tengjiqi@gmail.com` directly.