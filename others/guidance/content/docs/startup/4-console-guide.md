---
title: 任务管理与在线运维
type: docs
weight: 4
---

{{< hint info >}}
前端控制台允许开发者可视化地进行任务增、删、改、查等管理操作，同时也能直观地看到任务的运行数据，包括运行状态、详情和在线日志等。以下为对控制台的详细介绍：

{{< /hint >}}

## 主页

展示了系统整体的概览和集群Worker列表。

![main](/ohmyscheduler/oms-console-main.png)

## 任务创建

创建需要被调度执行的任务，入口为**主页 -> 任务管理 -> 新建任务**。

![jobcreator](/ohmyscheduler/oms-console-jobCreator.png)
* 任务名称：名称，便于记忆与搜索，无特殊用途，请尽量简短（占用数据库字段空间）
* 任务描述：描述，无特殊作用，请尽量简短（占用数据库字段空间）
* 任务参数：任务处理时能够获取到的参数（即各个Processor的process方法入参`TaskContext`对象的jobParams字段）（进行一次处理器开发就能理解了）
* 定时信息：由下拉框和输入框组成
    * API -> 不需要填写任何参数（填了也不起作用）
    * CRON -> 填写 CRON 表达式（可以找个[在线生成网站生成](https://www.bejson.com/othertools/cron/)）
    * 固定频率 -> 填写整数，单位毫秒
    * 固定延迟 -> 填写整数，单位毫秒
* 执行配置：由执行类型（单机、广播和MapReduce）、处理器类型和处理器参数组成，后两项相互关联。
    * 内置Java处理器 -> 填写该处理器的全限定类名（eg, `com.github.kfcfans.oms.processors.demo.MapReduceProcessorDemo`）
    * Java容器 -> 填写**容器ID#处理器全限定类名**（eg，`1#com.github.kfcfans.oms.container.DemoProcessor`）
    * SHELL -> 填写需要处理的脚本（直接复制文件内容）或脚本下载连接（http://xxx）
    * PYTHON -> 填写完整的python脚本或下载连接（http://xxx）
    
* 运行配置
    * 最大实例数：该任务同时执行的数量（任务和实例就像是类和对象的关系，任务被调度执行后被称为实例）
    * 单机线程并发数：该实例执行过程中每个Worker使用的线程数量（MapReduce任务生效，其余无论填什么，都只会使用1个线程或3个线程...）
    * 运行时间限制：限定任务的最大运行时间，超时则视为失败，单位**毫秒**，0代表不限制超时时间。

* 重试配置：
    * 任务重试次数：实例级别，失败了整个任务实例重试，会更换TaskTracker（本次任务实例的Master节点），代价较大，大型Map/MapReduce慎用。
    * 子任务重试次数：Task级别，每个子Task失败后单独重试，会更换ProcessorTracker（本次任务实际执行的Worker节点），代价较小，推荐使用。
    * 注：对于单机任务来说，假如任务重试次数和子任务重试次数都配置了1且都执行失败，实际执行次数会变成4次！推荐任务实例重试配置为0，子任务重试次数根据实际情况配置。

* 机器配置：用来标明允许执行任务的机器状态，避开那些摇摇欲坠的机器，0代表无任何限制。
    * 最低CPU核心数：填写浮点数，CPU可用核心数小于该值的Worker将不会执行该任务。
    * 最低内存（GB）：填写浮点数，可用内存小于该值的Worker将不会执行该任务。
    * 最低磁盘（GB）：填写浮点数，可用磁盘空间小于该值的Worker将不会执行该任务。
* 集群配置
    * 执行机器地址：指定集群中的某几台机器执行任务（debug的好帮手），多值英文逗号分割，如`192.168.1.1:27777,192.168.1.2:27777`
    * 最大执行机器数量：限定调动执行的机器数量

* 报警配置：选择任务执行失败后报警通知的对象，需要事先录入。

## 任务管理

直观地展示当前系统所管理的所有任务信息，并提供相应的运维方法。

![jobManager](/ohmyscheduler/oms-console-jobManager.png)

## 运行状态

直观地展示当前系统中运行任务实例的状态，点击详情即可获取详细的信息，点击日志可以查看通过`omsLogger`上报的日志，点击停止则可以强制终止该任务。

![status](/ohmyscheduler/oms-console-runningStatus.png)

## 在线日志

在线查看Worker执行过程中上报的日志，极大降低debug成本，提升开发效率！

![在线日志](/ohmyscheduler/oms-console-onlineLog.png)