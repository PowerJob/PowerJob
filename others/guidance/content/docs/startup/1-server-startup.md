---
title: 调度中心（Server）部署
weight: 1
---

## 环境要求

* Open JDK 8+

* Apache Maven 3+

* 任意 Spring Data Jpa 支持的关系型数据库（MySQL/Oracle/MS SQLServer...）
* MongoDB（可选），任意支持GridFS的mongoDB版本（4.2.6测试通过，其余未经测试，仅从理论角度分析可用），缺失该组件的情况下将无法使用在线日志、容器部署等扩展功能

## 初始化关系数据库

> 调度服务器（oh-my-scheduler-server）的持久化层基于`Spring Boot Jpa`实现，对于能够直连数据库的应用，开发者**仅需完成数据库的创建**，即运行SQL：CREATE database if NOT EXISTS oms-product default character set utf8mb4 collate utf8mb4_unicode_ci;`

* OhMyScheduler支持**环境隔离**，提供日常（daily）、预发（pre）和线上（product）三套环境，请根据使用的环境分别部署对应的数据库：`oms-daily`、`oms-pre`和`oms-product`。
* 调度服务器属于**时间敏感**应用，强烈建议检查当前数据库所使用的时区信息（`show variables like "%time_zone%";`），务必确保`time_zone`代表的时区与JDBC URL中`serverTimezone`字段代表的时区一致！
* 手动建表表SQL文件：[下载地址](https://github.com/KFCFans/OhMyScheduler/blob/master/others/oms-sql.sql)

## 部署调度服务器—源码编译

>调度服务器（oh-my-scheduler-server）支持任意的水平扩展，即多实例集群部署仅需要在同一个局域网内启动新的服务器实例，性能强劲无上限！

调度服务器（oh-my-scheduler-server）为了支持环境隔离，分别采用了日常（`application-daily.properties`）、预发（`application-pre.properties`）和线上（`application-product.properties`）三套配置文件，请根据实际需求进行修改，以下为配置文件详解。

| 配置项                         | 含义                                             | 可选                                   |
| ------------------------------ | ------------------------------------------------ | -------------------------------------- |
| server.port                    | SpringBoot配置，HTTP端口号，默认7700             | 否                                     |
| oms.akka.port                  | OhMyScheduler配置，Akka端口号，默认10086         | 否                                     |
| oms.alarm.bean.names           | OhMyScheduler报警服务Bean名称，多值逗号分隔      | 是                                     |
| spring.datasource.core.xxx     | 关系型数据库连接配置                             | 否                                     |
| spring.mail.xxx                | 邮件配置                                         | 是，未配置情况下将无法使用邮件报警功能 |
| spring.data.mongodb.xxx        | MongoDB连接配置                                  | 是，未配置情况下将无法使用在线日志功能 |
| oms.log.retention.local        | 本地日志保留天数，负数代表永久保留               | 否                                     |
| oms.log.retention.remote       | 远程日志保留天数，负数代表永久保留               | 否                                     |
| oms.container.retention.local  | 扩展的报警服务Bean，多值逗号分割，默认为邮件报警 | 否                                     |
| oms.container.retention.remote | 远程容器保留天数，负数代表永久保留               | 否                                     |

完成配置文件修改后，即可正式开始部署：

* 打包：运行`mvn clean package -U -Pdev -DskipTests`，构建调度中心Jar文件。
* 运行：运行`java -jar oms-server.jar --spring.profiles.active=product`，指定生效的配置文件。
* 验证：访问`http://ip:port`查看是否出现OhMyScheduler的欢迎页面。

## 部署调度服务器—Docker

> **建议自己根据项目中的Dockerfile稍作修改，制作自己的Docker镜像，而不是直接使用官方镜像**！原因在于：容器功能需要用到Git和Maven来编译代码库，而公司内部往往都会搭建自己的私有仓库，所以Git容器功能没办法正常运行（即，**官方镜像中的调度服务器不支持Git容器的部署**）。

[Docker Hub地址](https://hub.docker.com/r/tjqq/oms-server)

部署流程：

1. 下载镜像：`docker pull tjqq/oms-server`
2. 创建容器并运行（所有SpringBoot的启动参数都可通过`-e Params=""`传入）

```shell
docker run -d 
-e PARAMS="--spring.profiles.active=product" 
-p 7700:7700 -p 10086:10086 -p 27777:27777 
--name oms-server 
-v ~/docker/oms-server:/root/oms-server tjqq/oms-server:$version
```

## 单独部署前端页面（可选）

> 每一个oh-my-scheduler-server都自带了前端页面，不过~~~Tomcat~~~（为了完善的WebSocket支持，现已切换到Undertow）做Web服务器的性能就~~~呵呵了~~~（看评测好像还行，不过有追求的用户还是建议单独使用源码部署）～

1. 源码克隆：[OhMyScheduler-Console](https://github.com/KFCFans/OhMyScheduler-Console)
2. 替换地址：修改`main.js`中的`axios.defaults.baseURL`为服务器地址
3. npm run build -> nginx config

***

**特别鸣谢**：感谢[某知名上市电商公司前端](https://github.com/fengnan0929)对本项目的大力支持！

## 初始化应用分组

> 每一个业务系统初次接入OhMyScheduler时，都需要**先完成应用注册**。

![WelcomePage](/oms-console-welcome.png)

* 应用注册，用于进行业务分组：
  * 应用名称：关键参数，一般填入接入的业务应用名称即可，需要保证唯一。**同一个应用名称的所有worker视为一个集群被调度中心调度。**
  * 应用描述：可选参数，便于记忆，无实际用处。
* 用户注册，用于收集报警信息，用户注册录入个人信息后，即可通过报警配置进行通知。