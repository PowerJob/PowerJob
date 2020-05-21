---
title: 容器
weight: 1
---

## 什么是容器？

### 介绍

OhMyScheduler的容器技术允许开发者**开发独立于Worker项目之外Java处理器**，简单来说，就是以Maven工程项目的维度去组织一堆Java文件（开发者开发的众多脚本处理器），进而兼具开发效率和可维护性。

### 用途举例

* 比如，突然出现了某个数据库数据清理任务，与主业务无关，写进原本的项目工程中不太优雅，这时候就可以单独创建一个用于数据操作的容器，在里面完成处理器的开发，通过OhMyScheduler的容器部署技术在Worker集群上被加载执行。
* 比如，常见的日志清理啊，机器状态上报啊，对于广大Java程序员来说，也许并不是很会写shell脚本，此时也可以借用**agent+容器**技术，利用Java完成各项原本需要通过脚本进行的操作。

（感觉例子举的都不是很好...这个东西嘛，只可意会不可言传，大家努力理解一下吧～超好用哦～）

## 生成容器模版

{{< hint info >}}
为了方便开发者使用，最新版本的前端页面已经支持容器工程模版的自动生成，开发者仅需要填入相关信息即可下载容器模版开始开发。
{{< /hint >}}

![template](/ohmyscheduler/oms-console-container-template.png)

* Group：对应Maven的`<groupId>`标签，一般填入倒写的公司域名。
* Artifact：对于Maven的`<artifactId>`标签，填入代表该容器的唯一标示。
* Name：对应Maven的`<name>`标签，填入该容器名称。
* Package Name：包名，代表了的容器工程内部所使用的包名，**警告：包名一旦生成后，请勿更改！否则会导致运行时容器加载错误**（当然，如有必须修改包名的需求，可以尝试替换`/resource`下以`oms-worker-container`开头的所有文件相关的值）。
* Java Version：容器工程的Java版本，**请务必与容器目标部署Worker平台的Java版本保持一致**。

## 开发容器工程

完成容器模版创建后，下载，解压，会得到如下结构的Java工程：

```
oms-template-origin // 工程名称，可以自由更改
├── pom.xml
└── src
    ├── main
    │   ├── java
    │   │   └── cn
    │   │       └── edu
    │   │           └── zju
    │   │               └── tjq
    │   │                   └── container
    │   │                       └── samples // 所有处理器代码必须位于该目录下，其余类随意
    │   └── resources // 严禁随意更改以下两个配置文件（允许添加，不允许更改现有内容）
    │       ├── oms-worker-container-spring-context.xml
    │       └── oms-worker-container.properties
    └── test
        └── java
```

之后便可以愉快地在**packageName**下编写处理器代码啦～

需要示例代码？[客官这边请～](https://gitee.com/KFCFans/OhMyScheduler-Container-Template)

## 创建容器

目前，OhMyScheduler支持使用**Git代码库**和**FatJar**来创建容器。创建路径：**容器运维 -> 容器管理 -> 新建容器**。

{{< hint warning >}}
当使用**Git代码库**创建容器时，OhMyScheduler-Server需要完成代码库的下载、编译、构建和上传，**因此需要server运行环境包含可用的Git和Maven环境（包括私服的访问权限）**。
{{< /hint >}}

下图为使用**Git代码库**创建容器的示例，需要填入容器名称和代码库信息等参数：

![new-git-container](/ohmyscheduler/oms-console-container-newgit.png)

下图为使用**FatJar**创建容器的示例，需要上传可用的**FatJar**（注：FatJar值包含了所有依赖的Jar文件）：

![new-git-container](/ohmyscheduler/oms-console-container-newfatjar.png)

## 部署容器

{{< hint info >}}
完成容器创建后，便可在容器管理界面查看已创建的容器，点击**部署**，可以看到详细的部署信息。
{{< /hint >}}

![new-git-container](/ohmyscheduler/oms-console-container-deploy.png)

{{< hint info >}}
部署完成后，可以点击**机器列表**查看已部署该容器的机器信息。
{{< /hint >}}