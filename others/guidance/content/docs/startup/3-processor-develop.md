---
title: 处理器编写
weight: 3
---

## 处理器概述

> OhMyScheduler当前支持Shell、Python等脚本处理器和Java处理器。脚本处理器只需要开发者完成脚本的编写（xxx.sh / xxx.py），在控制台填入脚本内容即可，本章不再赘述。本章将重点阐述Java处理器开发方法与使用技巧。

* Java处理器可根据**代码所处位置**划分为内置Java处理器和容器Java处理器，前者直接集成在宿主应用（也就是接入本系统的业务应用）中，一般用来处理业务需求；后者可以在一个独立的轻量级的Java工程中开发，通过**容器技术**（详见容器章节）被worker集群热加载，提供Java的“脚本能力”，一般用于处理灵活多变的需求。
* Java处理器可根据**对象创建者**划分为SpringBean处理器和普通Java对象处理器，前者由Spring IOC容器完成处理器的创建和初始化，后者则有OhMyScheduler维护其状态。如果宿主应用支持Spring，**强烈建议使用SpringBean处理器**，开发者仅需要将Processor注册进Spring IOC容器（一个`@Component`注解或一句`bean`配置）。
* Java处理器可根据**功能**划分为单机处理器、广播处理器、Map处理器和MapReduce处理器。
  * 单机处理器对应了单机任务，即某个任务的某次运行只会有某一台机器的某一个线程参与运算。
  * 广播处理器对应了广播任务，即某个任务的某次运行会**调动集群内所有机器参与运算**。
  * Map处理器对应了Map任务，即某个任务在运行过程中，**允许产生子任务并分发到其他机器进行运算**。
  * MapReduce处理器对应了MapReduce任务，在Map任务的基础上，**增加了所有任务结束后的汇总统计**。

## 初始化宿主应用

首先，添加相关的jar包依赖，最新依赖版本请参考maven中央仓库：[推荐地址](https://search.maven.org/search?q=oh-my-scheduler-worker)&[备用地址](https://mvnrepository.com/search?q=com.github.kfcfans)

```xml
<dependency>
  <groupId>com.github.kfcfans</groupId>
  <artifactId>oh-my-scheduler-worker</artifactId>
  <version>1.2.0</version>
</dependency>
```



