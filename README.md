# English | [简体中文](./README_zhCN.md)

<p align="center">
🏮PowerJob 全体成员祝大家龙年腾飞，新的一年身体健康，万事如意，阖家欢乐，幸福安康！🏮
</p>

<p align="center">
<img src="https://raw.githubusercontent.com/KFCFans/PowerJob/master/others/images/logo.png" alt="PowerJob" title="PowerJob" width="557"/>
</p>

<p align="center">
<a href="https://github.com/PowerJob/PowerJob/actions"><img src="https://github.com/PowerJob/PowerJob/workflows/Java%20CI%20with%20Maven/badge.svg?branch=master" alt="actions"></a>
<a href="https://central.sonatype.com/search?smo=true&q=powerjob-worker&namespace=tech.powerjob"><img alt="Maven Central" src="https://img.shields.io/maven-central/v/tech.powerjob/powerjob-worker"></a>
<a href="https://github.com/PowerJob/PowerJob/releases"><img alt="GitHub release (latest SemVer)" src="https://img.shields.io/github/v/release/kfcfans/powerjob?color=%23E59866"></a>
<a href="https://github.com/PowerJob/PowerJob/blob/master/LICENSE"><img src="https://img.shields.io/github/license/KFCFans/PowerJob" alt="LICENSE"></a>
</p>

[PowerJob](https://github.com/PowerJob/PowerJob) is an open-source distributed computing and job scheduling framework which allows developers to easily schedule tasks in their own application.

Refer to [PowerJob Introduction](https://www.yuque.com/powerjob/en/introduce) for detailed information.

# Introduction

### Features
- **Friendly UI:** [Front-end](http://try.powerjob.tech/#/welcome?appName=powerjob-agent-test&password=123) page is provided and developers can manage their task, monitor the status, check the logs online, etc.

- **Abundant Timing Strategies:** Four timing strategies are supported, including CRON expression, fixed rate, fixed delay and OpenAPI which allows you to define your own scheduling policies, such as delaying execution.

- **Multiple Execution Mode:** Four execution modes are supported, including stand-alone, broadcast, Map and MapReduce. Distributed computing resource could be utilized in MapReduce mode, try the magic out [here](https://www.yuque.com/powerjob/en/za1d96#9YOnV)!

- **Workflow(DAG) Support:** Both job dependency management and data communications between jobs are supported.

- **Extensive Processor Support:** Developers can write their processors in Java, Shell, Python, and will subsequently support multilingual scheduling via HTTP.

- **Powerful Disaster Tolerance:** As long as there are enough computing nodes, configurable retry policies make it possible for your task to be executed and finished successfully.

- **High Availability & High Performance:**  PowerJob supports unlimited horizontal expansion. It's easy to achieve high availability and performance by deploying as many PowerJob server and worker nodes.

### Applicable scenes

- Timed tasks, for example, allocating e-coupons on 9 AM every morning.
- Broadcast tasks, for example, broadcasting to the cluster to clear logs.
- MapReduce tasks, for example, speeding up certain job like updating large amounts of data.
- Delayed tasks, for example, processing overdue orders.
- Customized tasks, triggered with [OpenAPI](https://www.yuque.com/powerjob/en/openapi).

### Online trial
- Address: [try.powerjob.tech](http://try.powerjob.tech/#/welcome?appName=powerjob-agent-test&password=123)
- Recommend reading the documentation first: [here](https://www.yuque.com/powerjob/en/trial)

# Documents
**[Docs](https://www.yuque.com/powerjob/en/introduce)**

**[中文文档](https://www.yuque.com/powerjob/guidence/intro)**

# Known Users
[Click to register as PowerJob user!](https://github.com/PowerJob/PowerJob/issues/6)  
ღ( ´・ᴗ・\` )ღ Many thanks to the following registered users. ღ( ´・ᴗ・\` )ღ
<p style="text-align: center">
<img src="https://raw.githubusercontent.com/KFCFans/PowerJob/master/others/images/user.png" alt="PowerJob User" title="PowerJob User"/>
</p>

# Stargazers over time

[![Stargazers over time](https://starchart.cc/PowerJob/PowerJob.svg)](https://starchart.cc/PowerJob/PowerJob)

# License

PowerJob is released under Apache License 2.0. Please refer to [License](./LICENSE) for details.

# Others

- Any developer interested in getting more involved in PowerJob may join our [Reddit](https://www.reddit.com/r/PowerJob) or [Gitter](https://gitter.im/PowerJob/community) community and make [contributions](https://github.com/PowerJob/PowerJob/pulls)!

- Reach out to me through email **tengjiqi@gmail.com**. Any issues or questions are welcomed on [Issues](https://github.com/PowerJob/PowerJob/issues).

- Look forward to your opinions. Response may be late but not denied.
