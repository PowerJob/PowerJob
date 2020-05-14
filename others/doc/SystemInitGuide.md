# STEP1: 调度中心部署 & 初始化
## 调度中心部署
#### 要求
* 运行环境：JDK8+
* 编译环境：Maven3+
* 关系数据库：任意Spring Data JPA支持的关系型数据库（MySQL/Oracle/MS SQLServer...）
* mongoDB（可选）：任意支持GridFS的mongoDB版本（4.2.6测试通过，其余未经测试，仅从理论角度分析可用）

#### 流程
1. 部署数据库：由于任务调度中心的数据持久层基于`Spring Data Jpa`实现，**开发者仅需要完成数据库的创建**，即运行SQL`CREATE database if NOT EXISTS oms-product default character set utf8mb4 collate utf8mb4_unicode_ci;`。
    * 注1：任务调度中心支持多环境部署（日常、预发、线上），其分别对应三个数据库：oms-daily、oms-pre和oms-product。
    * 注2：手动建表SQL文件：[oms-sql.sql](../oms-sql.sql)
    
2. 部署调度服务器（OhMyScheduler-Server），需要先修改配置文件（同样为了支持多环境部署，采用了daily、pre和product3套配置文件），之后自行编译部署运行。
    * 注1：OhMyScheduler-Server支持集群部署，具备完全的水平扩展能力。建议部署多个实例以实现高可用&高性能。
    * 注2：通过启动参数`--spring.profiles.active=product`来指定使用某套配置文件（默认为daily）
    * application-xxx.properties文件配置说明如下表所示：
    * |配置项|含义|可选|
      |----|----|----|
      |spring.datasource.core.xxx|关系型数据库连接配置|否|
      |spring.mail.xxx|邮件配置|是，未配置情况下将无法使用邮件报警功能|
      |spring.data.mongodb.xxx|MongoDB连接配置|是，未配置情况下将无法使用在线日志功能|
      |oms.log.retention.local|本地日志保留天数，负数代表永久保留|否|
      |oms.log.retention.remote|远程日志保留天数，负数代表永久保留|否|
      |oms.alarm.bean.names|扩展的报警服务Bean，多值逗号分割，默认为邮件报警|否|

3. 部署前端页面（可选）：每一个OhMyScheduler-Server内部自带了前端页面，不过Tomcat做Web服务器的性能就呵呵了～有需求（追求）的用户自行使用[源码](https://github.com/KFCFans/OhMyScheduler-Console)打包部署即可。
    * 需要修改`main.js`中的`axios.defaults.baseURL`为实际的OhMyScheduler-Server地址
    
## 初始化
> 每一个需要接入OhMyScheduler的系统，都需要先在控制台完成初始化，即应用注册与用户录入。初始化操作在首页完成。

![Welcome Page](../img/oms-console-welcome.png)
* 每一个系统初次接入OhMyScheduler时，都需要通过**应用注册**功能录入`appName`（接入应用的名称，需要保证唯一）和`appDescription`（描述信息，无实际用处），至此，应用初始化完成，准备开发处理器（Processor）享受分布式调度和计算的便利之处吧～
* 注册完成后，输入`appName`即可进入控制台。
* **用户注册**可录入用户信息，用于之后任务的报警配置。
